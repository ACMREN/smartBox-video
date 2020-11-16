package com.yangjie.JGB28181.web.controller;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import javax.sip.Dialog;
import javax.sip.SipException;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.searchCondition.DeviceBaseCondition;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.media.server.handler.TCPHandler;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.DeviceBaseInfoService;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.service.impl.PushHlsStreamServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.bean.DeviceChannel;
import com.yangjie.JGB28181.bean.PushStreamDevice;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.result.MediaData;
import com.yangjie.JGB28181.common.constants.ResultConstants;
import com.yangjie.JGB28181.media.callback.OnProcessListener;
import com.yangjie.JGB28181.media.server.Server;
import com.yangjie.JGB28181.media.server.TCPServer;
import com.yangjie.JGB28181.media.server.UDPServer;
import com.yangjie.JGB28181.media.server.remux.Observer;
import com.yangjie.JGB28181.media.server.remux.RtmpPusher;
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

	private MessageManager mMessageManager = MessageManager.getInstance();

	public static PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance();

	@Value("${config.pushHlsAddress}")
	private String pushHlsAddress;


	@Value("${config.pushRtmpAddress}")
	private String pushRtmpAddress;

	@Value("${config.checkSsrc}")
	private boolean checkSsrc;

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

	public static List<Integer> failCidList;

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
				result = this.playRtmp(deviceId);
				MediaData mediaData = (MediaData) result.getData();
				JSONObject data = new JSONObject();
				String address = mediaData.getAddress();
				String callId = mediaData.getCallId();
				data.put("deviceId", deviceId);
				data.put("source", address);
				resultList.add(data);

				this.handleStreamInfoMap(callId, deviceId, BaseConstants.PUSH_STREAM_RTMP);
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
					resultList.add(data);

					this.handleStreamInfoMap(callId, deviceId, BaseConstants.PUSH_STREAM_HLS);
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
			return this.GBPlayHls(cameraInfo);
		} else if (LinkTypeEnum.RTSP.getCode() == cameraInfo.getLinkType().intValue()) {
			// 如果摄像头注册方法只是onvif，那么用rtsp的方法进行推流
			String rtspLink = cameraInfo.getRtspLink();
			// 直接进行rtsp转hls推流
			return this.rtspToHls(rtspLink, deviceId.toString());
		}
		return null;
	}

	/**
	 * 国标播放hls视频
	 * @param cameraInfo
	 * @return
	 */
	private GBResult GBPlayHls(CameraInfo cameraInfo) {
		// 先进行rtmp的推流
		String cameraInfoIp = cameraInfo.getIp();
		GBResult rtmpResult = this.GBPlayRtmp(cameraInfoIp);
		int code = rtmpResult.getCode();
		if (code != 200){
			return rtmpResult;
		}

		JSONObject dataJson = this.getLiveCamInfoVoByMatchIp(cameraInfoIp);
		String deviceStr = dataJson.getString("deviceStr");
		String pushStreamDeviceId = dataJson.getString("pushStreamDeviceId");
		String channelId = null;
		if (!StringUtils.isEmpty(deviceStr)) {
			Device device = JSONObject.parseObject(deviceStr, Device.class);
			Map<String, DeviceChannel> channelMap = device.getChannelMap();
			for (String key : channelMap.keySet()) {
				DeviceChannel deviceChannel = channelMap.get(key);
				if (null != deviceChannel) {
					channelId = key;
				}
			}
		}
		// 再进行hls的推流
		return this.rtmpToHls(pushStreamDeviceId, channelId);
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
	private GBResult playRtmp(Integer deviceId) {
		CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceId);
		if (null != cameraInfo) {
			String linkType = LinkTypeEnum.getDataByCode(cameraInfo.getLinkType()).getName();
			String cameraIp = cameraInfo.getIp();
			if (LinkTypeEnum.GB28181.getName().equals(linkType)) {
				// 如果摄像头的注册类型是gb28181，那么就用国标的方式进行推流
				return this.GBPlayRtmp(cameraIp);
			} else if (LinkTypeEnum.RTSP.getName().equals(linkType)) {
				// 如果摄像头注册方法只是onvif，那么用rtsp的方法进行推流
				String rtspLink = cameraInfo.getRtspLink();
				CameraPojo cameraPojo = this.parseRtspLinkToCameraPojo(rtspLink);
				return this.rtspPlayRtmp(cameraPojo);
			}
		}
		return null;
	}

	/**
	 * 国标播放rtmp
	 * @param cameraIp
	 */
	private GBResult GBPlayRtmp(String cameraIp) {
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
			return this.play(null, pushStreamDeviceId, channelId, "TCP", 0, null);
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
			JSONObject typeStreamJson = streamJson.getJSONObject(type);
			if (null == typeStreamJson) {
				return GBResult.ok();
			}
			String callId = typeStreamJson.getString("callId");
			RedisUtil.expire(callId, 30*1000);
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
				} else if (LinkTypeEnum.RTSP.getName().equals(linkType)) {
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
			this.play(null, pushStreamDeviceId, channelId, "TCP", 1, cid);
		} else if (!StringUtils.isEmpty(rtspLink)) {
			// 把rtsp连接转成pojo
			CameraPojo cameraPojo = this.parseRtspLinkToCameraPojo(rtspLink);
			cameraPojo.setIsTest(1);
			cameraPojo.setCid(cid);
			// 测试播放rtsp转rtmp
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
			@RequestParam(value = "cid", required = false)Integer cid){
		GBResult result = null;
		try{
			int pushPort = 1935;
			//1.从redis查找设备，如果不存在，返回离线
			String deviceStr = RedisUtil.get(SipLayer.SUB_DEVICE_PREFIX + deviceId);
			if(StringUtils.isEmpty(deviceStr)){
				return GBResult.build(ResultConstants.DEVICE_OFF_LINE_CODE, ResultConstants.DEVICE_OFF_LINE);
			}
			//2.设备在线，先检查是否正在推流
			//如果正在推流，直接返回rtmp地址
			String streamName = StreamNameUtils.play(deviceId, channelId);
			PushStreamDevice pushStreamDevice = mPushStreamDeviceManager.get(streamName);
			if(pushStreamDevice != null){
				return GBResult.ok(new MediaData(pushStreamDevice.getPullRtmpAddress(),pushStreamDevice.getCallId()));
			}
			//检查通道是否存在
			Device device = JSONObject.parseObject(deviceStr, Device.class);
			Map<String, DeviceChannel> channelMap = device.getChannelMap();
			if(channelMap == null || !channelMap.containsKey(channelId)){
				return GBResult.build(ResultConstants.CHANNEL_NO_EXIST_CODE, ResultConstants.CHANNEL_NO_EXIST);
			}
			boolean isTcp = mediaProtocol.toUpperCase().equals(SipLayer.TCP);
			//3.下发指令
			String callId = IDUtils.id();
//			callId = "abc";
			//getPort可能耗时，在外面调用。
			int port = mSipLayer.getPort(isTcp);
			String ssrc = mSipLayer.getSsrc(true);
			mSipLayer.sendInvite(device,SipLayer.SESSION_NAME_PLAY,callId,channelId,port,ssrc,isTcp);
			//4.等待指令响应			
			SyncFuture<?> receive = mMessageManager.receive(callId);
			Dialog response = (Dialog) receive.get(3,TimeUnit.SECONDS);

			//4.1响应成功，创建推流session
			if(response != null ){
				String address = pushRtmpAddress.concat(streamName);
				Server server = isTcp ? new TCPServer() : new UDPServer();
				Observer observer = new RtmpPusher(address, callId);
				((RtmpPusher) observer).setDeviceId(streamName);
				
				server.subscribe(observer);
				pushStreamDevice = new PushStreamDevice(deviceId,Integer.valueOf(ssrc),callId,streamName,port,isTcp,server,
						observer,address);
				
				pushStreamDevice.setDialog(response);
				server.startServer(pushStreamDevice.getFrameDeque(),Integer.valueOf(ssrc),port,false, streamName);
				observer.startRemux(isTest, cid);

				observer.setOnProcessListener(this);
				mPushStreamDeviceManager.put(streamName, callId, Integer.valueOf(ssrc), pushStreamDevice);
				// 设置5分钟的过期时间
				RedisUtil.set(callId, 300, "keepStreaming");
				// 如果推流的id不为空且已经注册到数据库中，则保存在推流设备map中
				if (null != id && deviceManagerService.judgeCameraIsRegistered(id)) {
					streamingDeviceMap.put(id, pushStreamDevice);
				}
				result = GBResult.ok(new MediaData(pushStreamDevice.getPullRtmpAddress(),pushStreamDevice.getCallId()));
			}
			else {
				//3.2响应失败，删除推流session
				mMessageManager.remove(callId);
				result =  GBResult.build(ResultConstants.COMMAND_NO_RESPONSE_CODE, ResultConstants.COMMAND_NO_RESPONSE);
			}

		}catch(Exception e){
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
			// 获取当前时间
			String openTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime());
			Set<String> keys = CacheUtil.STREAMMAP.keySet();
			// 缓存是否为空
			if (0 == keys.size()) {
				// 开始推流
				cameraPojo = openStream(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(),
						pojo.getStream(), pojo.getStartTime(), pojo.getEndTime(), openTime, pojo.getCid());
				result = GBResult.ok(new MediaData(cameraPojo.getUrl(), cameraPojo.getToken()));
				logger.info("打开：" + cameraPojo.getRtsp());
			} else {
				// 是否存在的标志；0：不存在；1：存在
				int sign = 0;
				if (null == pojo.getStartTime()) {// 直播流
					for (String key : keys) {
						if (pojo.getIp().equals(CacheUtil.STREAMMAP.get(key).getIp())
								&& pojo.getChannel().equals(CacheUtil.STREAMMAP.get(key).getChannel())
								&& null == CacheUtil.STREAMMAP.get(key).getStartTime()) {// 存在直播流
							cameraPojo = CacheUtil.STREAMMAP.get(key);
							sign = 1;
							break;
						}
					}
					if (sign == 1) {// 存在
						cameraPojo.setCount(cameraPojo.getCount() + 1);
						cameraPojo.setOpenTime(openTime);
						result = GBResult.ok(new MediaData(cameraPojo.getUrl(), cameraPojo.getToken()));
						logger.info("打开：" + cameraPojo.getRtsp());
					} else {
						cameraPojo = openStream(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(),
								pojo.getStream(), pojo.getStartTime(), pojo.getEndTime(), openTime, pojo.getCid());
						result = GBResult.ok(new MediaData(cameraPojo.getUrl(), cameraPojo.getToken()));
						logger.info("打开：" + cameraPojo.getRtsp());
					}

				} else {// 历史流
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
						cameraPojo = openStream(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(),
								pojo.getStream(), pojo.getStartTime(), pojo.getEndTime(), openTime, pojo.getCid());
						result = GBResult.ok(new MediaData(cameraPojo.getUrl(), cameraPojo.getToken()));
						logger.info("打开：" + cameraPojo.getRtsp());
					}
				}
			}
		}
		return result;
	}

	public CameraPojo openStream(String ip, String username, String password, String channel, String stream,
								 String starttime, String endtime, String openTime, Integer cid) {
		CameraPojo cameraPojo = new CameraPojo();
		// 生成token
		String token = UUID.randomUUID().toString();
		String rtsp = "";
		String rtmp = "";
		String IP = IpUtil.IpConvert(ip);
		StringBuilder sb = new StringBuilder();
		String[] ipArr = ip.split("\\.");
		for (String item : ipArr) {
			sb.append(item);
		}
		token = sb.toString();
		String url = "";
		// 历史流
		if (!StringUtils.isEmpty(starttime)) {
			if (!StringUtils.isEmpty(endtime)) {
				rtsp = "rtsp://" + username + ":" + password + "@" + IP + ":554/Streaming/tracks/" + channel
						+ "01?starttime=" + starttime.substring(0, 8) + "t" + starttime.substring(8) + "z'&'endtime="
						+ endtime.substring(0, 8) + "t" + endtime.substring(8) + "z";
				cameraPojo.setStartTime(starttime);
				cameraPojo.setEndTime(endtime);
			} else {
				try {
					SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
					String startTime = df.format(df.parse(starttime).getTime() - 60 * 1000);
					String endTime = df.format(df.parse(starttime).getTime() + 60 * 1000);
					rtsp = "rtsp://" + username + ":" + password + "@" + IP + ":554/Streaming/tracks/" + channel
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
			rtsp = "rtsp://" + username + ":" + password + "@" + IP + ":554/h264/ch" + channel + "/" + stream
					+ "/av_stream";
			rtmp = "rtmp://" + IpUtil.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/live/" + token;
			if (config.getHost_extra().equals("127.0.0.1")) {
				url = rtmp;
			} else {
				url = "rtmp://" + IpUtil.IpConvert(config.getHost_extra()) + ":" + config.getPush_port() + "/live/"
						+ token;
			}
		}

		cameraPojo.setUsername(username);
		cameraPojo.setPassword(password);
		cameraPojo.setIp(IP);
		cameraPojo.setChannel(channel);
		cameraPojo.setStream(stream);
		cameraPojo.setRtsp(rtsp);
		cameraPojo.setRtmp(rtmp);
		cameraPojo.setUrl(url);
		cameraPojo.setOpenTime(openTime);
		cameraPojo.setCount(1);
		cameraPojo.setToken(token);
		cameraPojo.setCid(cid);

		// 执行任务
		CameraThread.MyRunnable job = new CameraThread.MyRunnable(cameraPojo);
		CameraThread.MyRunnable.es.execute(job);
		jobMap.put(token, job);
		// 设置5分钟的过期时间
		RedisUtil.set(token, 300, "keepStreaming");

		return cameraPojo;
	}

	@RequestMapping(value = "testSendRegister")
	public GBResult testSendRegister(String serverId, String serverDomain, String serverIp, String serverPort, String password, long cseq) {
		String callId = IDUtils.id();
		String fromTag = IDUtils.id();
		callId = "platform-" + callId;
		try	{
			mSipLayer.sendRegister(serverId, serverDomain, serverIp, serverPort, password, callId, fromTag, null, null, null, cseq);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return GBResult.ok();
	}

	/**
	 * 播放hls视频流，要基于rtmp视频流进行推送
	 * @param deviceId 设备id
	 * @param channelId 通道id
	 * @return
	 */
	@RequestMapping(value = "/rtmpToHls")
	public GBResult rtmpToHls(@RequestParam(value = "deviceId")String deviceId,
						  @RequestParam(value = "channelId")String channelId) {
		Boolean isAlive = PushHlsStreamServiceImpl.deviceInfoMap.get(Integer.valueOf(deviceId));
		if (isAlive != null && isAlive) {
			JSONObject dataJson = baseDeviceIdCallIdMap.get(deviceId);
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

	@Override
	public void onError(String callId) {
		try {
			mSipLayer.sendBye(callId);
		} catch (SipException e) {
			e.printStackTrace();
		}
	}
}
