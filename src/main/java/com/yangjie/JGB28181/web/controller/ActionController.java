package com.yangjie.JGB28181.web.controller;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.sip.Dialog;
import javax.sip.SipException;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.bean.*;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.utils.HCNetSDK;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.*;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.entity.enumEntity.HikvisionPTZCommandEnum;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.searchCondition.ControlCondition;
import com.yangjie.JGB28181.entity.searchCondition.DeviceBaseCondition;
import com.yangjie.JGB28181.entity.vo.FileCountInfoVo;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.entity.vo.RecordVideoInfoVo;
import com.yangjie.JGB28181.entity.vo.SnapshotInfoVo;
import com.yangjie.JGB28181.media.server.remux.*;
import com.yangjie.JGB28181.media.server.remux.Observer;
import com.yangjie.JGB28181.service.*;
import com.yangjie.JGB28181.service.impl.CameraControlServiceImpl;
import com.yangjie.JGB28181.service.impl.PushHlsStreamServiceImpl;
import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.result.MediaData;
import com.yangjie.JGB28181.common.constants.ResultConstants;
import com.yangjie.JGB28181.media.callback.OnProcessListener;
import com.yangjie.JGB28181.media.server.Server;
import com.yangjie.JGB28181.media.server.TCPServer;
import com.yangjie.JGB28181.media.server.UDPServer;
import com.yangjie.JGB28181.media.session.PushStreamDeviceManager;
import com.yangjie.JGB28181.message.SipLayer;
import com.yangjie.JGB28181.message.config.ConfigProperties;
import com.yangjie.JGB28181.message.session.MessageManager;
import com.yangjie.JGB28181.message.session.SyncFuture;


@RestController
@RequestMapping("/camera/")
@EnableConfigurationProperties(ConfigProperties.class)
public class ActionController implements OnProcessListener {
	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private SipLayer mSipLayer;

	@Autowired
	Config config;

	@Autowired
	private PushHlsStreamServiceImpl pushHlsStreamService;

	@Autowired
	private IDeviceManagerService deviceManagerService;

	@Autowired
	private CameraInfoService cameraInfoService;

	@Autowired
	private ICameraControlService cameraControlService;

	@Autowired
	private PresetInfoService presetInfoService;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private RecordVideoInfoService recordVideoInfoService;

	@Autowired
	private SnapshotInfoService snapshotInfoService;

	private MessageManager mMessageManager = MessageManager.getInstance();

	public static PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance();

	@Value("${config.pushHlsAddress}")
	private String pushHlsAddress;


	@Value("${config.pushRtmpAddress}")
	private String pushRtmpAddress;

	@Value("${config.checkSsrc}")
	private boolean checkSsrc;

	@Value("${config.streamMediaIp}")
	private String streamMediaIp;

	// 关闭推流的标志位
	private static volatile Map<String, Boolean> callEndMap = new HashMap<>(20);

	// 定时器执行线程池
	public static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

	public static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 3000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(10));

	// 保存国标rtmp转hls或flv的关系流
	public static Map<String, JSONObject> streamRelationMap = new HashMap<>(20);

	private static Map<String, Integer> callIdCountMap = new HashMap<>(20);

	// 判断设备是否正在推流
	public static Map<Integer, PushStreamDevice> streamingDeviceMap = new HashMap<>(20);

	// 保存设备基础id与推流id的关系
	public static Map<Integer, JSONObject> baseDeviceIdCallIdMap = new HashMap<>(20);

	// 存放任务 线程
	public static Map<String, CameraThread.MyRunnable> jobMap = new HashMap<String, CameraThread.MyRunnable>();

	// rtsp设备的拉流map
	public static Map<Integer, FrameGrabber> rtspDeviceGrabberMap = new HashMap<>(20);

	// 国标设备拉流器map
	public static Map<Integer, FrameGrabber> gbDeviceGrabberMap = new HashMap<>(20);

	// rtsp设备的线程处理器map
	public static Map<Integer, RtspToRtmpPusher> rtspPusherMap = new HashMap<>(20);

	// 国标设备录像map
	public static Map<Integer, RecordStreamDevice> deviceRecordMap = new HashMap<>(20);

	// 国标设备服务器map
	public static Map<String, Observer> gbServerMap = new HashMap<>(20);

	// 失败设备id列表
	public static List<Integer> failCidList = new ArrayList<>(20);

	// 截图标志位map
	public static Map<Integer, Boolean> deviceSnapshotMap = new HashMap<>(20);

	// 截图地址map
	public static Map<Integer, JSONObject> snapshotAddressMap = new HashMap<>();

	// 录像标志位map
	public static Map<Integer, Boolean> deviceRecordingMap = new HashMap<>(20);

	// 截图锁对象
	private static Object snapshotLock = new Object();

	// 录像锁对象
	private static Object recordLock = new Object();

	@PostMapping(value = "switchRecord")
	public GBResult switchRecord(@RequestBody DeviceBaseCondition deviceBaseCondition) throws InterruptedException {
		List<Integer> deviceIds = deviceBaseCondition.getDeviceIds();
		Integer isSwitch = deviceBaseCondition.getIsSwitch();

		// 关闭录像
		if (isSwitch == 0) {
//			List<Integer> failDeviceIds = this.stopRecordStream(deviceIds);
			synchronized (recordLock) {
				for (Integer deviceId : deviceIds) {
					deviceRecordingMap.put(deviceId, false);
				}
			}
		}

		// 开启录像
		GBResult result;
		if (isSwitch == 1) {
			synchronized (recordLock) {
				for (Integer deviceId : deviceIds) {
					deviceRecordingMap.put(deviceId, true);
				}
			}
		}

		return GBResult.ok();
	}

	private List<Integer> stopRecordStream(List<Integer> deviceBaseIds) {
		List<Integer> failDeviceIds = new ArrayList<>();
		if (!CollectionUtils.isEmpty(deviceBaseIds)) {
			for (Integer deviceBaseId : deviceBaseIds) {
				JSONObject streamJson = baseDeviceIdCallIdMap.get(deviceBaseId);
				JSONObject typeStreamJson = streamJson.getJSONObject(BaseConstants.PUSH_STREAM_RECORD);
				String callId = typeStreamJson.getString("callId");

				RtmpRecorder gbRecorder = (RtmpRecorder) ActionController.gbServerMap.get(callId);
				CameraThread.MyRunnable rtspRecorder = ActionController.jobMap.get(callId);
				if (gbRecorder != null) {
					gbRecorder.stopRemux();
					continue;
				}
				if (rtspRecorder != null) {
					rtspRecorder.setInterrupted();
					continue;
				}
				failDeviceIds.add(deviceBaseId);
			}
		}
		return failDeviceIds;
	}

	/**
	 * 获取视频流
	 * @param deviceBaseCondition
	 * @return
	 */
	@PostMapping(value = "getCameraStream")
	public GBResult getCameraStream(@RequestBody DeviceBaseCondition deviceBaseCondition) throws InterruptedException {
		List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
		String pushStreamType = deviceBaseCondition.getType();
		GBResult result = null;
		List<JSONObject> resultList = new ArrayList<>();
		if (BaseConstants.PUSH_STREAM_RTMP.equals(pushStreamType)) {
			for (Integer deviceId : deviceIds) {
				result = this.playRtmp(deviceId, 0);
				int resultCode = result.getCode();
				if (200 == resultCode) {
					MediaData mediaData = (MediaData) result.getData();
					JSONObject data = new JSONObject();
					String address = mediaData.getAddress();
					String callId = mediaData.getCallId();
					data.put("deviceId", deviceId);
					data.put("source", address);
					resultList.add(data);
					this.handleStreamInfoMap(callId, deviceId, BaseConstants.PUSH_STREAM_RTMP);
				} else {
					return result;
				}
			}
		} else if (BaseConstants.PUSH_STREAM_HLS.equals(pushStreamType)) {
			for (Integer deviceId : deviceIds) {
				CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceId);
				result = this.playHls(cameraInfo, deviceId);
				int resultCode = result.getCode();
				if (resultCode == 200) {
					MediaData mediaData = (MediaData) result.getData();
					JSONObject data = new JSONObject();
					String address = mediaData.getAddress();
					String callId = mediaData.getCallId();
					data.put("deviceId", deviceId);
					data.put("source", address);
					// 如果是hls推流，则等待m3u8文件生成再返回
					if (address.contains("m3u8")) {
						File indexFile = new File("/tmp/hls/" + address.split("/")[4] + "/index.m3u8");
						FileUtils.waitFileMade(indexFile);
					}
					resultList.add(data);

					this.handleStreamInfoMap(callId, deviceId, BaseConstants.PUSH_STREAM_HLS);
				} else {
					return result;
				}
			}
		} else if (BaseConstants.PUSH_STREAM_FLV.equals(pushStreamType)) {
			for (Integer deviceId : deviceIds) {
				result = this.playRtmp(deviceId, 1);
				int resultCode = result.getCode();
				if (200 == resultCode) {
					MediaData mediaData = (MediaData) result.getData();
					JSONObject data = new JSONObject();
					String address = mediaData.getAddress();
					String callId = mediaData.getCallId();
					data.put("deviceId", deviceId);
					data.put("source", address);
					resultList.add(data);
					this.handleStreamInfoMap(callId, deviceId, BaseConstants.PUSH_STREAM_RTMP);
				} else {
					return result;
				}
			}
		}
		return GBResult.ok(resultList);
	}

	private void handleStreamInfoMap(String callId, Integer deviceId, String type) {
		// 设置关闭推流的标志位为假
		callEndMap.put(callId, false);

		// 每请求一次对应的推流，则观看人数加一
		Integer count = callIdCountMap.get(callId);
		if (null == count) {
			count = 1;
		} else {
			count++;
		}
		callIdCountMap.put(callId, count);

		// 把推流的信息放入设备callId的map中
		JSONObject streamJson = baseDeviceIdCallIdMap.get(deviceId);
		if (null == streamJson) {
			streamJson = new JSONObject();
		}
		JSONObject typeStreamJson = new JSONObject();
		typeStreamJson.put("callId", callId);
		streamJson.put(type, typeStreamJson);
		baseDeviceIdCallIdMap.put(deviceId, streamJson);
	}

	/**
	 * 播放hls视频
	 * @param cameraInfo
	 * @param deviceId
	 * @return
	 */
	private GBResult playHls(CameraInfo cameraInfo, Integer deviceId) throws InterruptedException {
		if (LinkTypeEnum.GB28181.getCode() == cameraInfo.getLinkType().intValue()) {
			// 如果摄像头的注册类型是gb28181，那么就用国标的方式进行推流
			return this.GBPlayHls(cameraInfo.getIp(), deviceId);
		} else if (LinkTypeEnum.RTSP.getCode() == cameraInfo.getLinkType().intValue()) {
			// 如果摄像头注册方法只是onvif，那么用rtsp的方法进行推流
			String rtspLink = cameraInfo.getRtspLink();
			// 直接进行rtsp转hls推流
			CameraPojo cameraPojo = this.parseRtspLinkToCameraPojo(rtspLink);
			cameraPojo.setToHls(1);
			cameraPojo.setToFlv(0);
			cameraPojo.setDeviceId(deviceId.toString());
			cameraPojo.setIsRecord(0);
			cameraPojo.setIsSwitch(0);
			return this.rtspPlayRtmp(cameraPojo);
		}
		return null;
	}

	/**
	 * 把rtsp链接转成cameraPojo
	 * @param rtspLink
	 * @return
	 */
	private CameraPojo parseRtspLinkToCameraPojo(String rtspLink) {
		String paramStr = rtspLink.split("//")[1];
		String username = paramStr.split(":")[0];
		String paramStr1 = paramStr.split(":")[1];
		String password = paramStr1.split("@")[0];
		String ip = paramStr1.split("@")[1];
		String paramStr2 = paramStr.split("@")[1].split(":")[1];
		String channel = paramStr2.split("/")[2].split("ch")[1];
		String stream = paramStr2.split("/")[3];

		CameraPojo cameraPojo = new CameraPojo();
		cameraPojo.setUsername(username);
		cameraPojo.setPassword(password);
		cameraPojo.setIp(ip);
		cameraPojo.setChannel(channel);
		cameraPojo.setStream(stream);

		return cameraPojo;
	}

	/**
	 * 播放rtmp视频
	 * @param deviceId
	 * @return
	 */
	private GBResult playRtmp(Integer deviceId, Integer toFlv) {
		CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceId);
		if (null != cameraInfo) {
			String linkType = LinkTypeEnum.getDataByCode(cameraInfo.getLinkType()).getName();
			String cameraIp = cameraInfo.getIp();
			if (LinkTypeEnum.GB28181.getName().equals(linkType)) {
				// 如果摄像头的注册类型是gb28181，那么就用国标的方式进行推流
				return this.GBPlayRtmp(cameraIp, deviceId, 0, 0, toFlv);
			} else if (LinkTypeEnum.RTSP.getName().equals(linkType)) {
				// 如果摄像头注册方法只是onvif，那么用rtsp的方法进行推流
				String rtspLink = cameraInfo.getRtspLink();
				CameraPojo cameraPojo = this.parseRtspLinkToCameraPojo(rtspLink);
				cameraPojo.setToHls(0);
				cameraPojo.setToFlv(toFlv);
				cameraPojo.setDeviceId(deviceId.toString());
				cameraPojo.setIsRecord(0);
				cameraPojo.setIsSwitch(0);
				return this.rtspPlayRtmp(cameraPojo);
			}
		}
		return null;
	}

	/**
	 * 国标播放rtmp
	 * @param cameraIp
	 */
	private GBResult GBPlayRtmp(String cameraIp, Integer deviceId, Integer isRecord, Integer isSwitch, Integer toFlv) {
		JSONObject dataJson = this.getLiveCamInfoVoByMatchIp(cameraIp);
		String deviceStr = null;
		String pushStreamDeviceId = null;
		if (null != dataJson) {
			deviceStr = dataJson.getString("deviceStr");
			pushStreamDeviceId = dataJson.getString("pushStreamDeviceId");
		}
		// 如果redis上获取设备的信息成功，则进行推流
		if (!StringUtils.isEmpty(deviceStr)) {
			Device device = JSONObject.parseObject(deviceStr, Device.class);
			Map<String, DeviceChannel> channelMap = device.getChannelMap();
			String channelId = null;
			for (String key : channelMap.keySet()) {
				DeviceChannel deviceChannel = channelMap.get(key);
				if (null != deviceChannel) {
					channelId = key;
				}
			}
			return this.play(deviceId, pushStreamDeviceId, channelId, "TCP", 0, null, 0, isRecord, isSwitch, toFlv);
		}
		return GBResult.build(ResultConstants.CHANNEL_NO_EXIST_CODE, ResultConstants.CHANNEL_NO_EXIST);
	}

	private GBResult GBPlayHls(String cameraIp, Integer deviceId) {
		JSONObject dataJson = this.getLiveCamInfoVoByMatchIp(cameraIp);
		String deviceStr = null;
		String pushStreamDeviceId = null;
		if (null != dataJson) {
			deviceStr = dataJson.getString("deviceStr");
			pushStreamDeviceId = dataJson.getString("pushStreamDeviceId");
		}
		// 如果redis上获取设备的信息成功，则进行推流
		if (!StringUtils.isEmpty(deviceStr)) {
			Device device = JSONObject.parseObject(deviceStr, Device.class);
			Map<String, DeviceChannel> channelMap = device.getChannelMap();
			String channelId = null;
			for (String key : channelMap.keySet()) {
				DeviceChannel deviceChannel = channelMap.get(key);
				if (null != deviceChannel) {
					channelId = key;
				}
			}
			return this.play(deviceId, pushStreamDeviceId, channelId, "TCP", 0, null, 1, 0, 0, 0);
		}
		return GBResult.build(ResultConstants.CHANNEL_NO_EXIST_CODE, ResultConstants.CHANNEL_NO_EXIST);
	}

	/**
	 * 根据ip进行国标设备的匹配
	 * @param cameraIp
	 * @return
	 */
	private JSONObject getLiveCamInfoVoByMatchIp(String cameraIp) {
		List<LiveCamInfoVo> liveCamInfoVos = DeviceManagerController.liveCamVoList;
		String deviceStr = null;
		String pushStreamDeviceId = null;
		JSONObject dataJson = null;
		for (LiveCamInfoVo item : liveCamInfoVos) {
			String itemIp = item.getIp();
			// 如果ip匹配上，则从redis上获取设备的信息
			if (cameraIp.equals(itemIp)) {
				dataJson = new JSONObject();
				pushStreamDeviceId = item.getPushStreamDeviceId();
				deviceStr = RedisUtil.get(SipLayer.SUB_DEVICE_PREFIX + pushStreamDeviceId);

				dataJson.put("deviceStr", deviceStr);
				dataJson.put("pushStreamDeviceId", pushStreamDeviceId);
			}
		}
		return dataJson;
	}

	@PostMapping(value = "keepCameraStream")
	public GBResult keepCameraStream(@RequestBody DeviceBaseCondition deviceBaseCondition) {
		List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
		String type = deviceBaseCondition.getType();
		// 更新redis中推流key-value对象的时间，保证推流
		for (Integer deviceId : deviceIds) {
			JSONObject streamJson = ActionController.baseDeviceIdCallIdMap.get(deviceId);
			if (null == streamJson) {
				logger.info("获取设备推流信息失败，设备id=" + deviceId);
			}
			JSONObject typeStreamJson = streamJson.getJSONObject(type);
			if (null == typeStreamJson) {
				return GBResult.ok();
			}
			String callId = typeStreamJson.getString("callId");
			Long expiredMs = Long.valueOf(DeviceManagerController.cameraConfigBo.getStreamInterval());
			Integer expiredTime = Math.toIntExact(expiredMs / 1000);
			RedisUtil.expire(callId, expiredTime);
		}

		return GBResult.ok();
	}

	@PostMapping(value = "closeCameraStream")
	public GBResult closeCameraStream(@RequestBody DeviceBaseCondition deviceBaseCondition) {
		List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
		String type = deviceBaseCondition.getType();
		for (Integer deviceId : deviceIds) {
			CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceId);
			if (null == cameraInfo) {
				return GBResult.ok();
			}
			String linkType = LinkTypeEnum.getDataByCode(cameraInfo.getLinkType()).getName();
			JSONObject streamJson = baseDeviceIdCallIdMap.get(deviceId);
			if (null == streamJson) {
				return GBResult.ok();
			}
			JSONObject typeStreamJson = streamJson.getJSONObject(type);
			if (null == typeStreamJson) {
				return GBResult.ok();
			}
			String callId = typeStreamJson.getString("callId");
			// 判断观看人数是否已经为0
			Integer count = callIdCountMap.get(callId);
			if (null == count) {
				return GBResult.ok();
			}
			count--;
			if (count > 0) {
				callIdCountMap.put(callId, count);
				return GBResult.ok();
			}

			// 设置该推流的关闭标志位为真
			callEndMap.put(callId, true);
			// 设定两分钟的延时，如果在延时时间内有请求推流，则停止关闭推流
			scheduledExecutorService.schedule(() -> {
				Boolean endSymbol = callEndMap.get(callId);
				if (!endSymbol) {
					return;
				}
				logger.info("=======================关闭推流，开始================");
				if (LinkTypeEnum.GB28181.getName().equals(linkType)) {
					this.bye(callId);
				}
				if (LinkTypeEnum.RTSP.getName().equals(linkType)) {
					// 关闭hls推流
					this.rtspCloseCameraStream(callId);
					// 关闭rtmp推流
					CameraThread.MyRunnable rtspToRtmpRunnable = jobMap.get(callId);
					if (null != rtspToRtmpRunnable) {
						rtspToRtmpRunnable.setInterrupted();
						// 清除缓存
						CacheUtil.STREAMMAP.remove(callId);
						ActionController.jobMap.remove(callId);
						ActionController.baseDeviceIdCallIdMap.remove(deviceId);
					}
				}
				logger.info("=======================关闭推流，完成================");
			}, 2, TimeUnit.MINUTES);
		}
		return GBResult.ok();
	}

	public GBResult rtspCloseCameraStream(String callId) {
		Process hlsProcess = PushHlsStreamServiceImpl.hlsProcessMap.get(callId);
		if (null != hlsProcess && hlsProcess.isAlive()) {
			hlsProcess.destroy();

			// 删除文件夹及其内容
			JSONObject hlsInfoJSon = PushHlsStreamServiceImpl.hlsInfoMap.get(callId);
			String deviceId = hlsInfoJSon.getString("deviceId");
			String channelId = hlsInfoJSon.getString("channelId");
			String playFileName = StreamNameUtils.rtspPlay(deviceId, channelId);
			String filePath = BaseConstants.hlsStreamPath + playFileName;
			File dir = new File(filePath);
			if (!dir.isFile()) {
				File[] files = dir.listFiles();
				if (null != files) {
					for (File file : files) {
						file.delete();
					}
				}
			}
			dir.delete();

			// 删除hls推流信息
			PushHlsStreamServiceImpl.hlsProcessMap.remove(callId);
			PushHlsStreamServiceImpl.hlsInfoMap.remove(callId);
			PushHlsStreamServiceImpl.deviceInfoMap.remove(deviceId);
			ActionController.baseDeviceIdCallIdMap.remove(deviceId);
		}
		return GBResult.ok();
	}

	@PostMapping(value = "testCameraStream")
	public GBResult testCameraStream(@RequestParam(name = "pushStreamDeviceId", required = false)String pushStreamDeviceId,
									 @RequestParam(name = "channelId", required = false)String channelId,
									 @RequestParam(name = "rtspLink", required = false)String rtspLink,
									 @RequestParam(name = "deviceId")Integer cid,
									 @RequestParam(name = "type")String type) throws InterruptedException {
		failCidList = new ArrayList<>();
		if (!StringUtils.isEmpty(pushStreamDeviceId) && !StringUtils.isEmpty(channelId)) {
			this.play(null, pushStreamDeviceId, channelId, "TCP", 1, cid, 0, 0, 0, 0);
		} else if (!StringUtils.isEmpty(rtspLink)) {
			// 把rtsp连接转成pojo
			CameraPojo cameraPojo = this.parseRtspLinkToCameraPojo(rtspLink);
			cameraPojo.setIsTest(1);
			cameraPojo.setCid(cid);
			// 测试播放rtsp转rtmp
			cameraPojo.setIsRecord(0);
			cameraPojo.setIsSwitch(0);
			cameraPojo.setToFlv(0);
			this.rtspPlayRtmp(cameraPojo);
		}
		// 等待5秒，结果返回
		Thread.sleep(5 * 1000);

		return GBResult.ok(ActionController.failCidList);
	}

	/**
	 * 播放rtmp基础视频流
	 * @param deviceId 设备id
	 * @param channelId 通道id
	 * @param mediaProtocol 推流协议，默认为tcp
	 * @return
	 */
	@RequestMapping("play")
	public GBResult play(
			@RequestParam(value = "id", required = false)Integer id,
			@RequestParam(value = "deviceId", required = false)String deviceId,
			@RequestParam(value = "channelId", required = false)String channelId,
			@RequestParam(value = "protocol", required = false, defaultValue = "TCP")String mediaProtocol,
			@RequestParam(value = "isTest", defaultValue = "0")Integer isTest,
			@RequestParam(value = "cid", required = false)Integer cid,
			@RequestParam(value = "toHls", required = false)Integer toHls,
			@RequestParam(value = "isRecord", required = false)Integer isRecord,
			@RequestParam(value = "isSwitch", required = false)Integer isSwitch,
			@RequestParam(value = "toFlv", required = false)Integer toFlv){
		GBResult result = null;
		try{
			int pushPort = 1935;
			//1.从redis查找设备，如果不存在，返回离线
			String deviceStr = RedisUtil.get(SipLayer.SUB_DEVICE_PREFIX + deviceId);
			if(StringUtils.isEmpty(deviceStr)){
				return GBResult.build(ResultConstants.DEVICE_OFF_LINE_CODE, ResultConstants.DEVICE_OFF_LINE);
			}
			// 2.设备在线，先检查是否正在推流
			// 如果正在推流，直接返回rtmp地址
			String streamName = StreamNameUtils.play(deviceId, channelId);
			PushStreamDevice pushStreamDevice = mPushStreamDeviceManager.get(streamName);
			RecordStreamDevice recordStreamDevice = ActionController.deviceRecordMap.get(id);
			if(pushStreamDevice != null && isRecord == 0){
				if (toFlv == 0) {
					return GBResult.ok(new MediaData(pushStreamDevice.getPullRtmpAddress(),pushStreamDevice.getCallId()));
				} else {
					return GBResult.ok(new MediaData(pushStreamDevice.getPullFlvAddress(),pushStreamDevice.getCallId()));
				}
			}
			if (recordStreamDevice != null && isRecord == 1) {
				return GBResult.build(201, "已经正在录像，请勿重复请求", null);
			}
			boolean isTcp = mediaProtocol.toUpperCase().equals(SipLayer.TCP);
			int port = mSipLayer.getPort(isTcp);
			// 检查通道是否存在
			Device device = JSONObject.parseObject(deviceStr, Device.class);
			Map<String, DeviceChannel> channelMap = device.getChannelMap();
			if(channelMap == null || !channelMap.containsKey(channelId)){
				return GBResult.build(ResultConstants.CHANNEL_NO_EXIST_CODE, ResultConstants.CHANNEL_NO_EXIST);
			}
			// 3.下发指令
			String callId = null;
			if (isRecord == 0) {
				callId = IDUtils.id();
			} else {
				callId = "record_" + IDUtils.id();
			}
			// getPort可能耗时，在外面调用。
			String ssrc = mSipLayer.getSsrc(true);
			mSipLayer.sendInvite(device,SipLayer.SESSION_NAME_PLAY,callId,channelId,port,ssrc,isTcp);
			// 4.等待指令响应
			SyncFuture<?> receive = mMessageManager.receive(callId);
			Dialog response = (Dialog) receive.get(3,TimeUnit.SECONDS);

			//4.1响应成功，创建推流session
			if(response != null){
				String address = pushRtmpAddress.concat(streamName);
				// 如果是hls，则推到hls的地址
				if (toHls == 1) {
					address = pushHlsAddress.concat(streamName);
				}
				Server server = isTcp ? new TCPServer() : new UDPServer();
				Observer observer;

				// 判断是推流还是录像
				if (isRecord == 0) {
					observer = new RtmpPusher(address, callId);
					((RtmpPusher) observer).setDeviceId(streamName);
					String pullFlvAddress = BaseConstants.flvBaseUrl + streamName;
					// 保存推流信息
					pushStreamDevice = new PushStreamDevice(deviceId,Integer.valueOf(ssrc),callId,streamName,port,isTcp,server,
							observer,address, pullFlvAddress);
					pushStreamDevice.setDialog(response);
					mPushStreamDeviceManager.put(streamName, callId, Integer.valueOf(ssrc), pushStreamDevice);
				} else {
					String recordAddress = RecordNameUtils.recordVideoFileAddress(streamName);
					observer = new RtmpRecorder(recordAddress, callId);
					((RtmpRecorder) observer).setDeviceId(streamName);
					recordStreamDevice = new RecordStreamDevice(deviceId, Integer.valueOf(ssrc), callId, streamName, port, isTcp, server,
							observer, recordAddress);
					deviceRecordMap.put(id, recordStreamDevice);
				}

				server.subscribe(observer);
				server.startServer(new ConcurrentLinkedDeque<>(),Integer.valueOf(ssrc),port,false, streamName, id);
				observer.startRemux(isTest, cid, toHls, id, streamName, applicationContext);
				ActionController.gbServerMap.put(callId, observer);

				observer.setOnProcessListener(this);
				if (isRecord == 0) {
					Long expiredMs = Long.valueOf(DeviceManagerController.cameraConfigBo.getStreamInterval());
					Integer expiredTime = Math.toIntExact(expiredMs / 1000);
					// 设置5分钟的过期时间
					RedisUtil.set(callId, expiredTime, "keepStreaming");
					// 如果推流的id不为空且已经注册到数据库中，则保存在推流设备map中
					if (null != id && deviceManagerService.judgeCameraIsRegistered(id)) {
						streamingDeviceMap.put(id, pushStreamDevice);
					}
					if (toHls == 1) {
						// 开启清理过期的TS索引文件的定时器
						PushHlsStreamServiceImpl.cleanUpTempTsFile(deviceId, channelId, 0);

						String mediaIp = PushHlsStreamServiceImpl.getStreamMediaIp();
						String pushHlsStreamAddress = BaseConstants.hlsBaseUrl.replace("127.0.0.1", mediaIp);
						String hlsPlayFile = pushHlsStreamAddress + streamName + "/index.m3u8";
						pushStreamDevice.setPullRtmpAddress(hlsPlayFile);
						result = GBResult.ok(new MediaData(hlsPlayFile, pushStreamDevice.getCallId()));
					} else {
						result = GBResult.ok(new MediaData(pushStreamDevice.getPullRtmpAddress(),pushStreamDevice.getCallId()));
					}
				} else {
					result = GBResult.ok(new MediaData(recordStreamDevice.getPullRtmpAddress(), recordStreamDevice.getCallId()));
				}
			}
			else {
				//3.2响应失败，删除推流session
				mMessageManager.remove(callId);
				result =  GBResult.build(ResultConstants.COMMAND_NO_RESPONSE_CODE, ResultConstants.COMMAND_NO_RESPONSE);
			}
		} catch(Exception e){
			e.printStackTrace();
			result = GBResult.build(ResultConstants.SYSTEM_ERROR_CODE, ResultConstants.SYSTEM_ERROR);
		}
		return result;
	}

	/**
	 * rtsp转rtmp视频流播放
	 * @param pojo
	 * @return
	 */
	@PostMapping(value = "/rtspToRtmpPlay")
	public GBResult rtspPlayRtmp(CameraPojo pojo) {
		// 返回结果
		Map<String, String> map = new HashMap<String, String>();
		GBResult result  = null;
		// 校验参数
		if (!StringUtils.isEmpty(pojo.getIp()) && !StringUtils.isEmpty(pojo.getUsername()) && !StringUtils.isEmpty(pojo.getPassword())
				&& !StringUtils.isEmpty(pojo.getChannel())) {
			CameraPojo cameraPojo = new CameraPojo();
			cameraPojo.setToHls(pojo.getToHls());
			// 获取当前时间
			String openTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime());
			Set<String> keys = CacheUtil.STREAMMAP.keySet();
			// 缓存是否为空
			if (0 == keys.size()) {
				// 开始推流
				cameraPojo = openStream(pojo);
				String url = cameraPojo.getUrl();
				if (pojo.getToHls() == 1) {
					url = cameraPojo.getHlsUrl();
				}
				if (pojo.getIsRecord() == 1) {
					url = cameraPojo.getRecordDir();
				}
				if (pojo.getToFlv() == 1) {
					url = cameraPojo.getFlv();
				}
				result = GBResult.ok(new MediaData(url, cameraPojo.getToken()));
				logger.info("打开：" + cameraPojo.getRtsp());
			} else {
				// 是否存在的标志；0：不存在；1：存在
				int sign = 0;
				if (null == pojo.getStartTime() && pojo.getIsRecord() == 0) {// 直播流
					for (String key : keys) {
						if (pojo.getIp().equals(CacheUtil.STREAMMAP.get(key).getIp())
								&& pojo.getChannel().equals(CacheUtil.STREAMMAP.get(key).getChannel())
								&& null == CacheUtil.STREAMMAP.get(key).getStartTime()) {// 存在直播流
							cameraPojo = CacheUtil.STREAMMAP.get(key);
							sign = 1;
							break;
						}
					}
					if (sign == 1 && pojo.getIsRecord() == 0) {// 存在
						cameraPojo.setCount(cameraPojo.getCount() + 1);
						cameraPojo.setOpenTime(openTime);
						String url = cameraPojo.getUrl();
						if (pojo.getToHls() == 1) {
							url = cameraPojo.getHlsUrl();
						}
						if (pojo.getToFlv() == 1) {
							url = cameraPojo.getFlv();
						}
						result = GBResult.ok(new MediaData(url, cameraPojo.getToken()));
						logger.info("打开：" + cameraPojo.getRtsp());
					} else {
						cameraPojo = openStream(pojo);
						String url = cameraPojo.getUrl();
						if (pojo.getToHls() == 1) {
							url = cameraPojo.getHlsUrl();
						}
						if (pojo.getToFlv() == 1) {
							url = cameraPojo.getFlv();
						}
						result = GBResult.ok(new MediaData(url, cameraPojo.getToken()));
						logger.info("打开：" + cameraPojo.getRtsp());
					}
				} else if (null != pojo.getStartTime() && pojo.getIsRecord() == 0){// 历史流
					for (String key : keys) {
						if (pojo.getIp().equals(CacheUtil.STREAMMAP.get(key).getIp())
								&& null != CacheUtil.STREAMMAP.get(key).getStartTime()) {// 存在历史流
							cameraPojo = CacheUtil.STREAMMAP.get(key);
							sign = 1;
							break;
						}
					}
					if (sign == 1) {
						cameraPojo.setCount(cameraPojo.getCount() + 1);
						cameraPojo.setOpenTime(openTime);
						map.put("message", "正在进行回放...");
						logger.info(cameraPojo.getRtsp() + " 正在进行回放...");
					} else {
						cameraPojo = openStream(pojo);
						result = GBResult.ok(new MediaData(cameraPojo.getUrl(), cameraPojo.getToken()));
						logger.info("打开：" + cameraPojo.getRtsp());
					}
				} else if (pojo.getIsRecord() == 1) {
					for (String key : keys) {
						if (pojo.getIp().equals(CacheUtil.STREAMMAP.get(key).getIp())
								&& pojo.getChannel().equals(CacheUtil.STREAMMAP.get(key).getChannel())
								&& null == CacheUtil.STREAMMAP.get(key).getStartTime()) {// 存在直播流
							cameraPojo = CacheUtil.STREAMMAP.get(key);
							sign = 1;
							break;
						}
					}
					if (sign == 1 && pojo.getIsRecord() == 1) {// 存在
						String recordDir = cameraPojo.getRecordDir();
						result = GBResult.ok(new MediaData(recordDir, cameraPojo.getToken()));
						logger.info("打开：" + cameraPojo.getRtsp());
					} else {
						cameraPojo = openStream(pojo);
						String url = cameraPojo.getRecordDir();
						result = GBResult.ok(new MediaData(url, cameraPojo.getToken()));
						logger.info("打开：" + cameraPojo.getRtsp());
					}
				}
			}
		}
		return result;
	}

	public CameraPojo openStream(CameraPojo pojo) {
		CameraPojo cameraPojo = new CameraPojo();
		// 生成token
		String token = UUID.randomUUID().toString();
		String rtsp = "";
		String rtmp = "";
		String hls = "";
		String flv = "";
		String hlsUrl = "";
		String IP = IpUtil.IpConvert(pojo.getIp());
		StringBuilder sb = new StringBuilder();
		String[] ipArr = pojo.getIp().split("\\.");
		for (String item : ipArr) {
			sb.append(item);
		}
		sb.append(pojo.getChannel());
		token = sb.toString();
		if (pojo.getIsRecord() == 1) {
			token = "record_" + sb.toString();
		}
		String url = "";
		// 历史流
		if (!StringUtils.isEmpty(pojo.getStartTime())) {
			if (!StringUtils.isEmpty(pojo.getEndTime())) {
				rtsp = "rtsp://" + pojo.getUsername() + ":" + pojo.getPassword() + "@" + IP + ":554/Streaming/tracks/" + pojo.getChannel()
						+ "01?starttime=" + pojo.getStartTime().substring(0, 8) + "t" + pojo.getStartTime().substring(8) + "z'&'endtime="
						+ pojo.getEndTime().substring(0, 8) + "t" + pojo.getEndTime().substring(8) + "z";
				cameraPojo.setStartTime(pojo.getStartTime());
				cameraPojo.setEndTime(pojo.getEndTime());
			} else {
				try {
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
					String startTime = df.format(df.parse(pojo.getStartTime()).getTime() - 60 * 1000);
					String endTime = df.format(df.parse(pojo.getEndTime()).getTime() + 60 * 1000);
					rtsp = "rtsp://" + pojo.getUsername() + ":" + pojo.getPassword() + "@" + IP + ":554/Streaming/tracks/" + pojo.getChannel()
							+ "01?starttime=" + startTime.substring(0, 8) + "t" + startTime.substring(8)
							+ "z'&'endtime=" + endTime.substring(0, 8) + "t" + endTime.substring(8) + "z";
					cameraPojo.setStartTime(startTime);
					cameraPojo.setEndTime(endTime);
				} catch (ParseException e) {
					logger.error("时间格式化错误！", e);
				}
			}
			rtmp = "rtmp://" + IpUtil.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/history/"
					+ token;
			if (config.getHost_extra().equals("127.0.0.1")) {
				url = rtmp;
			} else {
				url = "rtmp://" + IpUtil.IpConvert(config.getHost_extra()) + ":" + config.getPush_port() + "/history/"
						+ token;
			}
		} else {// 直播流
			if (pojo.getToHls() == 1) {
				// 开启清理过期的TS索引文件的定时器
				PushHlsStreamServiceImpl.cleanUpTempTsFile(pojo.getDeviceId(), "1", 1);
			}
			rtsp = "rtsp://" + pojo.getUsername() + ":" + pojo.getPassword() + "@" + IP + ":554/h264/ch" + pojo.getChannel() + "/" + pojo.getStream()
					+ "/av_stream";
			rtmp = "rtmp://" + IpUtil.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/live/" + token;
			hls = "rtmp://" + IpUtil.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/hls/" + StreamNameUtils.rtspPlay(pojo.getDeviceId(), "1");
			flv	= BaseConstants.flvBaseUrl + token;
			if (config.getHost_extra().equals("127.0.0.1")) {
				hlsUrl = BaseConstants.hlsBaseUrl + StreamNameUtils.rtspPlay(pojo.getDeviceId(), "1") + "/index.m3u8";
				hlsUrl = hlsUrl.replace("127.0.0.1", streamMediaIp);
				url = rtmp;
				url = url.replace("127.0.0.1", streamMediaIp);
				flv = flv.replace("127.0.0.1", streamMediaIp);
			} else {
				url = "rtmp://" + IpUtil.IpConvert(config.getHost_extra()) + ":" + config.getPush_port() + "/live/"
						+ token;
			}
		}

		String recordDir = RecordNameUtils.recordVideoFileAddress(StreamNameUtils.rtspPlay(pojo.getDeviceId(), "1"));
		cameraPojo.setUsername(pojo.getUsername());
		cameraPojo.setPassword(pojo.getPassword());
		cameraPojo.setIp(IP);
		cameraPojo.setChannel(pojo.getChannel());
		cameraPojo.setStream(pojo.getStream());
		cameraPojo.setRtsp(rtsp);
		cameraPojo.setRtmp(rtmp);
		cameraPojo.setHls(hls);
		cameraPojo.setUrl(url);
		cameraPojo.setFlv(flv);
		cameraPojo.setHlsUrl(hlsUrl);
		cameraPojo.setOpenTime(pojo.getOpenTime());
		cameraPojo.setCount(1);
		cameraPojo.setToken(token);
		cameraPojo.setCid(pojo.getCid());
		cameraPojo.setToHls(pojo.getToHls());
		cameraPojo.setToFlv(pojo.getToFlv());
		cameraPojo.setDeviceId(pojo.getDeviceId());
		cameraPojo.setIsRecord(pojo.getIsRecord());
		cameraPojo.setIsSwitch(pojo.getIsSwitch());
		cameraPojo.setRecordDir(recordDir);
		cameraPojo.setApplicationContext(applicationContext);

		// 执行任务
		CameraThread.MyRunnable job = new CameraThread.MyRunnable(cameraPojo);
		CameraThread.MyRunnable.es.execute(job);
		jobMap.put(token, job);
		if (cameraPojo.getIsRecord() == 0) {
			Long expiredMs = Long.valueOf(DeviceManagerController.cameraConfigBo.getStreamInterval());
			Integer expiredTime = Math.toIntExact(expiredMs / 1000);
			// 设置5分钟的过期时间
			RedisUtil.set(token, expiredTime, "keepStreaming");


		}

		return cameraPojo;
	}

	/**
	 * 播放hls视频流，要基于rtmp视频流进行推送
	 * @param deviceId 设备id
	 * @param channelId 通道id
	 * @return
	 */
	@RequestMapping(value = "/rtmpToHls")
	public GBResult rtmpToHls(@RequestParam(value = "deviceId")String deviceId,
							  @RequestParam(value = "channelId")String channelId,
							  @RequestParam(value = "id", required = false)Integer id) {
		Boolean isAlive = PushHlsStreamServiceImpl.deviceInfoMap.get(deviceId);
		if (isAlive != null && isAlive) {
			JSONObject dataJson = baseDeviceIdCallIdMap.get(id);
			String callId = dataJson.getString("callId");
			String playFileName = StreamNameUtils.play(deviceId, channelId);
			String hlsBaseUrl = BaseConstants.hlsBaseUrl;
			hlsBaseUrl = hlsBaseUrl.replace("127.0.0.1", config.getStreamMediaIp());
			return GBResult.ok(new MediaData(hlsBaseUrl + playFileName + "/index.m3u8", callId));
		}
		return pushHlsStreamService.pushStream(deviceId, channelId);
	}

	/**
	 * 获取hls推流的所有信息
	 * @return
	 */
	@RequestMapping(value = "getHlsStreamInfo")
	public GBResult getHlsStreamInfo() {
		JSONObject infoJson = new JSONObject();
		infoJson.put("hlsInfoMap", PushHlsStreamServiceImpl.hlsInfoMap);
		return GBResult.ok(infoJson);
	}

	/**
	 * rtsp转hls进行视频推流
	 * @param rtspLink
	 * @param deviceId
	 * @return
	 */
	@RequestMapping(value = "rtspToHls")
	public GBResult rtspToHls(@RequestParam(value = "rtspLink")String rtspLink,
							  @RequestParam(value = "deviceId")String deviceId) {
		Boolean isAlive = PushHlsStreamServiceImpl.deviceInfoMap.get(deviceId);
		if (isAlive != null && isAlive) {
			JSONObject dataJson = baseDeviceIdCallIdMap.get(Integer.valueOf(deviceId));
			String callId = dataJson.getString("callId");
			String playFileName = StreamNameUtils.rtspPlay(deviceId, "1");
			String hlsBaseUrl = BaseConstants.hlsBaseUrl;
			hlsBaseUrl = hlsBaseUrl.replace("127.0.0.1", config.getStreamMediaIp());
			return GBResult.ok(new MediaData(hlsBaseUrl + playFileName + "/index.m3u8", callId));
		}
		return pushHlsStreamService.rtspPushStream(deviceId, "1", rtspLink);
	}

	/**
	 * 关闭hls视频流
	 * @param callId
	 * @return
	 */
	@RequestMapping(value = "closeHls")
	public GBResult closeHls(@RequestParam(value = "callId")String callId) {
		pushHlsStreamService.closeStream(callId);

		return GBResult.ok();
	}

	/**
	 * 主动搜索onvif摄像头
	 * @return
	 */
	@RequestMapping(value = "discoverDevice")
	public GBResult discoverDevice() {
		Set<String> result = deviceManagerService.discoverDevice();
		return GBResult.ok(result);
	}

	@RequestMapping("bye")
	public GBResult bye(@RequestParam("callId")String callId){
		try {
			JSONObject subCallIdJson = streamRelationMap.get(callId);
			if (null != subCallIdJson) {
				// 如果包含hls推流callId，就先关闭子推流
				String hlsCallId = subCallIdJson.getString("hls");
				if (!StringUtils.isEmpty(hlsCallId)) {
					this.closeHls(hlsCallId);
				}
			}
			mSipLayer.sendBye(callId);
		} catch (SipException e) {
			e.printStackTrace();
		}
		return GBResult.ok();
	}

	/**
	 * 测试云台控制
	 * @return
	 */
	@RequestMapping("PTZMoveControlTest")
	public GBResult PTZMoveControlTest(@RequestBody ControlCondition controlCondition) {
		String producer = controlCondition.getProducer();
		String ip = controlCondition.getIp();
		Integer port = controlCondition.getPort();
		String userName = controlCondition.getUserName();
		String password = controlCondition.getPassword();
		JSONObject PTZParams = controlCondition.getPTZParams();
		String command = PTZParams.getString("command");
		Integer speed = PTZParams.getInteger("speed");
		Integer isStop = PTZParams.getInteger("isStop");

		cameraControlService.cameraMove(producer, ip, 8000, userName, password, command, speed, isStop);

		return GBResult.ok();
	}

	/**
	 * 已注册的设备进行云台操作
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("PTZMoveControl")
	public GBResult PTZMoveControl(@RequestBody ControlCondition controlCondition) {
		Integer deviceId = controlCondition.getDeviceId();
		String direction = controlCondition.getDirection();
		Integer speed = controlCondition.getSpeed();
		List<Integer> deviceIds = new ArrayList<>();
		deviceIds.add(deviceId);

		DeviceBaseInfo deviceBaseInfo = deviceManagerService.getDeviceBaseInfoList(deviceIds).get(0);
		CameraInfo cameraInfo = deviceManagerService.getCameraInfoList(deviceIds).get(0);
		String specification = deviceBaseInfo.getSpecification();
		// 1. 验证设备信息
		GBResult verifyResult = this.verifyDeviceInfo(specification, cameraInfo);
		int verifyCode = verifyResult.getCode();
		if (verifyCode != 200) {
			return verifyResult;
		}
		CameraPojo rtspPojo = (CameraPojo) verifyResult.getData();

		// 4. 如果速度为0，则相当于停止
		Integer isStop = 0;
		if (speed == 0) {
			isStop = 1;
		}

		cameraControlService.cameraMove(specification, rtspPojo.getIp(), 8000, rtspPojo.getUsername(), rtspPojo.getPassword(), direction, speed, isStop);

		return GBResult.ok();
	}

	/**
	 * 获取云台位置
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("getPTZStatus")
	public GBResult getPTZStatus(@RequestBody ControlCondition controlCondition) {
		Integer deviceBaseId = controlCondition.getDeviceId();
		List<Integer> deviceIds = new ArrayList<>();
		deviceIds.add(deviceBaseId);

		DeviceBaseInfo deviceBaseInfo = deviceManagerService.getDeviceBaseInfoList(deviceIds).get(0);
		CameraInfo cameraInfo = deviceManagerService.getCameraInfoList(deviceIds).get(0);
		String specification = deviceBaseInfo.getSpecification();
		// 1. 验证设备信息
		GBResult verifyResult = this.verifyDeviceInfo(specification, cameraInfo);
		int verifyCode = verifyResult.getCode();
		if (verifyCode != 200) {
			return verifyResult;
		}
		CameraPojo rtspPojo = (CameraPojo) verifyResult.getData();

		GBResult posResult = cameraControlService.getDVRConfig(specification, rtspPojo.getIp(), 8000, rtspPojo.getUsername(), rtspPojo.getPassword(), HCNetSDK.NET_DVR_SET_PTZPOS);
		JSONObject posJson = (JSONObject) posResult.getData();
		Integer pPos = posJson.getInteger("p");
		Integer tPos = posJson.getInteger("t");
		Integer zPos = posJson.getInteger("z");
		// 转换结果
		posJson.put("p", CameraControlServiceImpl.HexToDecMa(pPos.shortValue()));
		posJson.put("t", CameraControlServiceImpl.HexToDecMa(tPos.shortValue()));
		posJson.put("z", CameraControlServiceImpl.HexToDecMa(zPos.shortValue()));

		return GBResult.ok(posJson);
	}

	/**
	 * 插入/更新预置点
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("setPTZPreset")
	public GBResult setPTZPreset(@RequestBody ControlCondition controlCondition) {
		Integer deviceBaseId = controlCondition.getDeviceId();
		JSONObject psConfig = controlCondition.getPsConfig();
		List<Integer> deviceIds = new ArrayList<>();
		deviceIds.add(deviceBaseId);

		DeviceBaseInfo deviceBaseInfo = deviceManagerService.getDeviceBaseInfoList(deviceIds).get(0);
		CameraInfo cameraInfo = deviceManagerService.getCameraInfoList(deviceIds).get(0);
		String specification = deviceBaseInfo.getSpecification();
		// 1. 验证设备信息
		GBResult verifyResult = this.verifyDeviceInfo(specification, cameraInfo);
		int verifyCode = verifyResult.getCode();
		if (verifyCode != 200) {
			return verifyResult;
		}
		CameraPojo rtspPojo = (CameraPojo) verifyResult.getData();

		Integer psId = psConfig.getInteger("psId");
		String psName = psConfig.getString("psName");

		// 2. 获取当前点位信息
		GBResult posResult = cameraControlService.getDVRConfig(specification, rtspPojo.getIp(), 8000, rtspPojo.getUsername(), rtspPojo.getPassword(), HCNetSDK.NET_DVR_SET_PTZPOS);
		JSONObject posJson = (JSONObject) posResult.getData();
		Integer pPos = posJson.getInteger("p");
		Integer tPos = posJson.getInteger("t");
		Integer zPos = posJson.getInteger("z");
		JSONObject presetPos = new JSONObject();
		presetPos.put("pPos", pPos.shortValue());
		presetPos.put("tPos", tPos.shortValue());
		presetPos.put("zPos", zPos.shortValue());

		// 5. 插入或更新数据库
		PresetInfo presetInfo = new PresetInfo();
		presetInfo.setId(psId);
		presetInfo.setDeviceBaseId(deviceBaseId);
		presetInfo.setPresetName(psName);
		presetInfo.setPresetPos(presetPos.toJSONString());
		presetInfoService.saveOrUpdate(presetInfo);

		return GBResult.ok();
	}

	/**
	 * 删除预置点
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("delPTZPerset")
	public GBResult delPTZPerset(@RequestBody ControlCondition controlCondition) {
		Integer deviceBaseId = controlCondition.getDeviceId();
		List<Integer> presetIds = controlCondition.getPsIds();
		List<Integer> deviceIds = new ArrayList<>();
		deviceIds.add(deviceBaseId);

		presetInfoService.removeByIds(presetIds);

		return GBResult.ok();
	}

	/**
	 * 获取设备对应的预置点
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("getPTZPreset")
	public GBResult getPTZPreset(@RequestBody ControlCondition controlCondition) {
		Integer deviceBaseId = controlCondition.getDeviceId();
		List<Integer> deviceIds = new ArrayList<>();
		deviceIds.add(deviceBaseId);

		List<JSONObject> resultList = new ArrayList<>();
		List<PresetInfo> presetInfoList = presetInfoService.list(new QueryWrapper<PresetInfo>().in("device_base_id", deviceIds));
		if (!CollectionUtils.isEmpty(presetInfoList)) {
			for (PresetInfo item : presetInfoList) {
				JSONObject data = new JSONObject();
				data.put("psId", item.getId());
				data.put("deviceId", item.getDeviceBaseId());
				data.put("psName", item.getPresetName());
				JSONObject posJson = JSONObject.parseObject(item.getPresetPos());
				data.put("p", posJson.getString("pPos"));
				data.put("t", posJson.getString("tPos"));
				data.put("z", posJson.getString("zPos"));
				resultList.add(data);
			}
		}

		return GBResult.ok(resultList);
	}

	/**
	 * 把云台移动对应的预置点
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("toPTZPreset")
	public GBResult toPTZPreset(@RequestBody ControlCondition controlCondition) {
		Integer deviceBaseId = controlCondition.getDeviceId();
		Integer psId = controlCondition.getPsIds().get(0);
		List<Integer> deviceIds = new ArrayList<>();
		deviceIds.add(deviceBaseId);
		System.out.println("psId : " + psId);

		DeviceBaseInfo deviceBaseInfo = deviceManagerService.getDeviceBaseInfoList(deviceIds).get(0);
		CameraInfo cameraInfo = deviceManagerService.getCameraInfoList(deviceIds).get(0);
		String specification = deviceBaseInfo.getSpecification();
		// 1. 验证设备信息
		GBResult verifyResult = this.verifyDeviceInfo(specification, cameraInfo);
		int verifyCode = verifyResult.getCode();
		if (verifyCode != 200) {
			return verifyResult;
		}
		CameraPojo rtspPojo = (CameraPojo) verifyResult.getData();

		// 2. 控制云台移动到指定的预置点
		PresetInfo presetInfo = presetInfoService.getById(psId);
		if (null != presetInfo) {
			String presetPos = presetInfo.getPresetPos();
			JSONObject presetPosJson = JSONObject.parseObject(presetPos);

			return cameraControlService.setDVRConfig(specification, rtspPojo.getIp(), 8000, rtspPojo.getUsername(),
					rtspPojo.getPassword(), HCNetSDK.NET_DVR_SET_PTZPOS, presetPosJson);
		}
		return GBResult.fail();
	}

	/**
	 * 控制云台图像放大
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("setPTZZoom")
	public GBResult setPTZZoom(@RequestBody ControlCondition controlCondition) {
		Integer deviceBaseId = controlCondition.getDeviceId();
		List<Integer> deviceIds = new ArrayList<>();
		JSONObject region = controlCondition.getRegion();
		deviceIds.add(deviceBaseId);

		DeviceBaseInfo deviceBaseInfo = deviceManagerService.getDeviceBaseInfoList(deviceIds).get(0);
		CameraInfo cameraInfo = deviceManagerService.getCameraInfoList(deviceIds).get(0);
		String specification = deviceBaseInfo.getSpecification();
		// 1. 验证设备信息
		GBResult verifyResult = this.verifyDeviceInfo(specification, cameraInfo);
		int verifyCode = verifyResult.getCode();
		if (verifyCode != 200) {
			return verifyResult;
		}
		CameraPojo rtspPojo = (CameraPojo) verifyResult.getData();

		// 2. 控制云台指定区域放大
		cameraControlService.NET_DVR_PTZSelZoomIn(specification, rtspPojo.getIp(), 8000, rtspPojo.getUsername(), rtspPojo.getPassword(), region);

		return GBResult.ok();
	}

	/**
	 * 按照日期统计录像/截图数量和文件大小
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("countFileByDate")
	public GBResult countFileByDate(@RequestBody ControlCondition controlCondition) {
		List<Integer> deviceBaseIds = controlCondition.getDeviceIds();
		String beginTime = controlCondition.getBegin();
		String endTime = controlCondition.getEnd();

		// 1. 找出数据并进行组装
		List<FileCountInfoVo> resultList = new ArrayList<>();
		Map<String, FileCountInfoVo> resultMap = new HashMap<>();
		for (Integer deviceBaseId : deviceBaseIds) {
			// 1.1. 数据库找出数据
			List<FileCountInfo> recordCountInfos = recordVideoInfoService.countDataByDate(deviceBaseId, beginTime, endTime);
			List<FileCountInfo> snapshotCountInfos = snapshotInfoService.countDataByDate(deviceBaseId, beginTime, endTime);


			// 1.2. 找出的数据转化成日期和数据的map
			Map<String, FileCountInfo> recordCountInfoMap = recordCountInfos.stream().collect(Collectors.toMap(FileCountInfo::getDate, Function.identity()));
			Map<String, FileCountInfo> snapshotCountInfoMap = snapshotCountInfos.stream().collect(Collectors.toMap(FileCountInfo::getDate, Function.identity()));

			// 1.3. 把录像文件的信息放入到结果map中
			for (String key : recordCountInfoMap.keySet()) {
				FileCountInfoVo data = new FileCountInfoVo();
				data.setRecordCount(recordCountInfoMap.get(key).getCount());
				data.setRecordSize(recordCountInfoMap.get(key).getFileSize());
				data.setTimestamp(key);
				resultMap.put(recordCountInfoMap.get(key).getDate(), data);
			}

			// 1.4. 把截图文件的信息放入到结果map中
			for (String key : snapshotCountInfoMap.keySet()) {
				FileCountInfoVo data = resultMap.get(key);
				if (null == data) {
					data = new FileCountInfoVo();
					data.setTimestamp(key);
				}
				data.setSnapshotCount(snapshotCountInfoMap.get(key).getCount());
				data.setSnapshotSize(snapshotCountInfoMap.get(key).getFileSize());
			}
		}

		// 2. 放入结果列表
		for (String key : resultMap.keySet()) {
			resultList.add(resultMap.get(key));
		}

		return GBResult.ok(resultList);
	}

	/**
	 * 根据日期查询截图
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("getSnapshot")
	public GBResult getSnapshot(@RequestBody ControlCondition controlCondition) {
		List<Integer> deviceBaseIds = controlCondition.getDeviceIds();
		String beginTime = controlCondition.getBegin();
		String endTime = controlCondition.getEnd();
		Integer pageSize = controlCondition.getPageSize();
		Integer pageNo = controlCondition.getPageNo();

		Integer offset = (pageNo - 1) * pageSize;

		List<SnapshotInfo> snapshotInfos = snapshotInfoService.getBaseMapper()
				.selectList(new QueryWrapper<SnapshotInfo>()
						.in("device_base_id", deviceBaseIds)
						.gt("create_time", beginTime)
						.lt("create_time", endTime)
						.last("limit " + offset + ", " + pageSize));

		Integer total = snapshotInfoService.getBaseMapper()
				.selectCount(new QueryWrapper<SnapshotInfo>()
						.in("device_base_id", deviceBaseIds)
						.gt("create_time", beginTime)
						.lt("create_time", endTime));

		List<SnapshotInfoVo> resultList = new ArrayList<>();
		for (SnapshotInfo snapshotInfo : snapshotInfos) {
			SnapshotInfoVo snapshotInfoVo = new SnapshotInfoVo(snapshotInfo);
			resultList.add(snapshotInfoVo);
		}

		PageListVo pageListVo = new PageListVo(resultList, pageNo, pageSize, total);

		return GBResult.ok(pageListVo);
	}

	/**
	 * 根据日期查询录像
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("getRecord")
	public GBResult getRecord(@RequestBody ControlCondition controlCondition) {
		List<Integer> deviceBaseIds = controlCondition.getDeviceIds();
		String beginTime = controlCondition.getBegin();
		String endTime = controlCondition.getEnd();
		Integer pageSize = controlCondition.getPageSize();
		Integer pageNo = controlCondition.getPageNo();

		Integer offset = (pageNo - 1) * pageSize;

		List<RecordVideoInfo> recordVideoInfos = recordVideoInfoService.getBaseMapper()
				.selectList(new QueryWrapper<RecordVideoInfo>()
						.in("device_base_id", deviceBaseIds)
						.gt("start_time", beginTime)
						.lt("end_time", endTime)
						.last("limit " + offset + "," + pageSize));

		Integer total = recordVideoInfoService.getBaseMapper()
				.selectCount(new QueryWrapper<RecordVideoInfo>()
						.in("device_base_id", deviceBaseIds)
						.gt("start_time", beginTime)
						.lt("end_time", endTime));

		List<RecordVideoInfoVo> resultList = new ArrayList<>();
		for (RecordVideoInfo item : recordVideoInfos) {
			RecordVideoInfoVo data = new RecordVideoInfoVo(item);
			resultList.add(data);
		}

		PageListVo pageListVo = new PageListVo(resultList, pageNo, pageSize, total);

		return GBResult.ok(pageListVo);
	}

	/**
	 * 视频截图
	 * @param controlCondition
	 * @return
	 */
	@RequestMapping("grabSnapshot")
	public GBResult grabSnapShot(@RequestBody ControlCondition controlCondition) throws InterruptedException {
		synchronized (snapshotLock) {
			Integer deviceBaseId = controlCondition.getDeviceIds().get(0);

			deviceSnapshotMap.put(deviceBaseId, true);

			// 等待截图完成，并获取截图地址
			FileUtils.waitSnapshot(deviceBaseId, ActionController.deviceSnapshotMap.get(deviceBaseId));
			JSONObject snapshotAddressJson = ActionController.snapshotAddressMap.get(deviceBaseId);

			return GBResult.ok(snapshotAddressJson);
		}
	}

	/**
	 * 验证设备的信息
	 * @param specification
	 * @param cameraInfo
	 * @return
	 */
	private GBResult verifyDeviceInfo(String specification, CameraInfo cameraInfo) {
		// 1. 验证设备是否具有具体型号
		if (StringUtils.isEmpty(specification)) {
			return GBResult.build(500, "设备无法进行操作，原因：设备没有具体型号", null);
		}
		// 2. 验证设备是否通过rtsp方式进行注册
		if (LinkTypeEnum.RTSP.getCode() != cameraInfo.getLinkType().intValue()) {
			return GBResult.build(500, "设备无法进行操作，原因：设备没有设置rtsp链接", null);
		}
		// 3. 获取rtsp链接并转成对象
		String rtspLink = cameraInfo.getRtspLink();
		CameraPojo rtspPojo = this.parseRtspLinkToCameraPojo(rtspLink);

		return GBResult.ok(rtspPojo);
	}

	@Override
	public void onError(String callId) {
		try {
			mSipLayer.sendBye(callId);
		} catch (SipException e) {
			e.printStackTrace();
		}
	}
}
