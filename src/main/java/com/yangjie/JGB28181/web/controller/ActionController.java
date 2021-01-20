package com.yangjie.JGB28181.web.controller;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sip.Dialog;
import javax.sip.SipException;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.bean.*;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.utils.HCNetSDK;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.*;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.entity.condition.GBDevicePlayCondition;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.condition.ControlCondition;
import com.yangjie.JGB28181.entity.condition.DeviceBaseCondition;
import com.yangjie.JGB28181.entity.vo.FileCountInfoVo;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.entity.vo.RecordVideoInfoVo;
import com.yangjie.JGB28181.entity.vo.SnapshotInfoVo;
import com.yangjie.JGB28181.media.server.handler.TestClientHandler;
import com.yangjie.JGB28181.media.server.handler.TestClientUDPHandler;
import com.yangjie.JGB28181.media.server.remux.*;
import com.yangjie.JGB28181.media.server.remux.Observer;
import com.yangjie.JGB28181.service.*;
import com.yangjie.JGB28181.service.impl.CameraControlServiceImpl;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
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

	@Autowired
	private ICameraRecordService cameraRecordService;

	private MessageManager mMessageManager = MessageManager.getInstance();

	public static PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance();

	@Value("${config.pushHlsAddress}")
	private String pushHlsAddress;


	@Value("${config.pushRtmpAddress}")
	private String pushRtmpAddress;

	// 截图锁对象
	private static Object snapshotLock = new Object();

	@PostMapping(value = "switchRecord")
	public GBResult switchRecord(@RequestBody DeviceBaseCondition deviceBaseCondition) throws SipException {
		List<Integer> deviceIds = deviceBaseCondition.getDeviceIds();
		Integer isSwitch = deviceBaseCondition.getIsSwitch();

		// 关闭录像
		if (isSwitch == 0) {
			List<Integer> failDeviceIds = cameraRecordService.stopRecordStream(deviceIds);
			if (CollectionUtils.isEmpty(failDeviceIds)) {
				return GBResult.ok();
			} else {
				return GBResult.build(500, "部分设备关闭录像失败", failDeviceIds);
			}
		}

		// 开启录像
		if (isSwitch == 1) {
			cameraRecordService.startCameraRecord(deviceIds);
		}

		return GBResult.ok();
	}

	/**
	 * 获取视频流
	 * @param deviceBaseCondition
	 * @return
	 */
	@PostMapping(value = "getCameraStream")
	public GBResult getCameraStream(@RequestBody DeviceBaseCondition deviceBaseCondition) throws Exception {
		List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
		String pushStreamType = deviceBaseCondition.getType();
		Integer toHigherServer = deviceBaseCondition.getToHigherServer();
		GBResult result = null;
		List<JSONObject> resultList = new ArrayList<>();
		// 播放rtmp
		if (BaseConstants.PUSH_STREAM_RTMP.equals(pushStreamType)) {
			for (Integer deviceId : deviceIds) {
				result = this.playPushStream(deviceId, 0, 0);
				if (null != handlePlayStreamResult(result, deviceId, resultList)) {
					return result;
				}
			}
			// 播放hls
		} else if (BaseConstants.PUSH_STREAM_HLS.equals(pushStreamType)) {
			for (Integer deviceId : deviceIds) {
				CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceId);
				result = this.playPushStream(deviceId, 0, 1);
				if (null != handlePlayStreamResult(result, deviceId, resultList)) {
					return result;
				}
			}
			// 播放flv
		} else if (BaseConstants.PUSH_STREAM_FLV.equals(pushStreamType)) {
			for (Integer deviceId : deviceIds) {
				result = this.playPushStream(deviceId, 1, 0);
				if (null != handlePlayStreamResult(result, deviceId, resultList)) {
					return result;
				}
			}
		}
		return GBResult.ok(resultList);
	}

	/**
	 * 处理播放视频的结果
	 * @param result
	 * @param deviceId
	 * @param resultList
	 * @return
	 * @throws InterruptedException
	 */
	private GBResult handlePlayStreamResult(GBResult result, Integer deviceId, List<JSONObject> resultList) throws InterruptedException {
		int resultCode = result.getCode();
		if (200 == resultCode) {
			MediaData mediaData = (MediaData) result.getData();
			JSONObject data = new JSONObject();
			String address = mediaData.getAddress();
			String callId = mediaData.getCallId();
			data.put("deviceId", deviceId);
			data.put("source", address);
			if (address.contains("m3u8")) {
				File indexFile = new File("/tmp/hls/" + address.split("/")[4] + "/index.m3u8");
				FileUtils.waitFileMade(indexFile);
			}
			resultList.add(data);
			StreamUtils.handleStreamInfoMap(callId, deviceId, BaseConstants.PUSH_STREAM_RTMP);
			return null;
		} else {
			return result;
		}
	}

	/**
	 * 播放推流视频
	 * @param deviceId
	 * @return
	 */
	private GBResult playPushStream(Integer deviceId, Integer toFlv, Integer toHls) {
		CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceId);
		if (null != cameraInfo) {
			String cameraIp = cameraInfo.getIp();
			if (LinkTypeEnum.GB28181.getCode() == cameraInfo.getLinkType().intValue()) {
				String deviceSerialNum = cameraInfo.getDeviceSerialNum();
				String parentSerialNum = cameraInfo.getParentSerialNum();
				// 如果摄像头的注册类型是gb28181，那么就用国标的方式进行推流
				return cameraInfoService.gbPlay(parentSerialNum, deviceSerialNum, cameraIp, deviceId, 0, 0, toFlv, toHls);
			} else if (LinkTypeEnum.RTSP.getCode() == cameraInfo.getLinkType().intValue()) {
				// 如果摄像头注册方法只是onvif，那么用rtsp的方法进行推流
				String rtspLink = cameraInfo.getRtspLink();
				CameraPojo cameraPojo = DeviceUtils.parseRtspLinkToCameraPojo(rtspLink);
				cameraPojo.setToHls(toHls);
				cameraPojo.setToFlv(toFlv);
				cameraPojo.setDeviceId(deviceId.toString());
				cameraPojo.setIsRecord(0);
				cameraPojo.setIsSwitch(0);
				return cameraInfoService.rtspDevicePlay(cameraPojo);
			}
		}
		return null;
	}

	@PostMapping(value = "keepCameraStream")
	public GBResult keepCameraStream(@RequestBody DeviceBaseCondition deviceBaseCondition) {
		List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
		String type = deviceBaseCondition.getType();
		// 更新redis中推流key-value对象的时间，保证推流
		for (Integer deviceId : deviceIds) {
			JSONObject streamJson = CacheUtil.baseDeviceIdCallIdMap.get(deviceId);
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
			JSONObject streamJson = CacheUtil.baseDeviceIdCallIdMap.get(deviceId);
			if (null == streamJson) {
				return GBResult.ok();
			}
			JSONObject typeStreamJson = streamJson.getJSONObject(type);
			if (null == typeStreamJson) {
				return GBResult.ok();
			}
			String callId = typeStreamJson.getString("callId");
			// 判断观看人数是否已经为0
			Integer count = CacheUtil.callIdCountMap.get(callId);
			if (null == count) {
				return GBResult.ok();
			}
			count--;
			if (count > 0) {
				CacheUtil.callIdCountMap.put(callId, count);
				return GBResult.ok();
			}

			// 设置该推流的关闭标志位为真
			CacheUtil.callEndMap.put(callId, true);
			// 设定两分钟的延时，如果在延时时间内有请求推流，则停止关闭推流
			CacheUtil.scheduledExecutorService.schedule(() -> {
				Boolean endSymbol = CacheUtil.callEndMap.get(callId);
				if (!endSymbol) {
					return;
				}
				logger.info("=======================关闭推流，开始================");
				if (LinkTypeEnum.GB28181.getName().equals(linkType)) {
					this.bye(callId);
				}
				if (LinkTypeEnum.RTSP.getName().equals(linkType)) {
					// 关闭hls推流
//					this.rtspCloseCameraStream(callId);
					// 关闭rtmp推流
					CameraThread.MyRunnable rtspToRtmpRunnable = CacheUtil.jobMap.get(callId);
					if (null != rtspToRtmpRunnable) {
						rtspToRtmpRunnable.setInterrupted();
						// 清除缓存
						CacheUtil.STREAMMAP.remove(callId);
						CacheUtil.jobMap.remove(callId);
						CacheUtil.baseDeviceIdCallIdMap.remove(deviceId);
					}
				}
				logger.info("=======================关闭推流，完成================");
			}, 2, TimeUnit.MINUTES);
		}
		return GBResult.ok();
	}

	@PostMapping(value = "testCameraStream")
	public GBResult testCameraStream(@RequestParam(name = "pushStreamDeviceId", required = false)String pushStreamDeviceId,
									 @RequestParam(name = "channelId", required = false)String channelId,
									 @RequestParam(name = "rtspLink", required = false)String rtspLink,
									 @RequestParam(name = "deviceId")Integer cid,
									 @RequestParam(name = "type")String type) throws Exception {
		CacheUtil.failCidList = new ArrayList<>();
		if (!StringUtils.isEmpty(pushStreamDeviceId) && !StringUtils.isEmpty(channelId)) {
			this.play(new GBDevicePlayCondition(null, pushStreamDeviceId, channelId, "TCP", 1, cid, 0, 0, 0, 0, 1, 0, null, null, null));
		} else if (!StringUtils.isEmpty(rtspLink)) {
			// 把rtsp连接转成pojo
			CameraPojo cameraPojo = DeviceUtils.parseRtspLinkToCameraPojo(rtspLink);
			cameraPojo.setIsTest(1);
			cameraPojo.setCid(cid);
			// 测试播放rtsp转rtmp
			cameraPojo.setIsRecord(0);
			cameraPojo.setIsSwitch(0);
			cameraPojo.setToFlv(0);
			this.rtspDevicePlay(cameraPojo);
		}

		return GBResult.ok(CacheUtil.failCidList);
	}

	/**
	 * 播放rtmp基础视频流
	 * @param gbDevicePlayCondition
	 * @return
	 */
	@RequestMapping("play")
	public GBResult play(GBDevicePlayCondition gbDevicePlayCondition) throws Exception {
		GBResult result = null;
		return cameraInfoService.gbDevicePlay(gbDevicePlayCondition);
	}

	/**
	 * rtsp转rtmp视频流播放
	 * @param pojo
	 * @return
	 */
	@PostMapping(value = "/rtspDevicePlay")
	public GBResult rtspDevicePlay(CameraPojo pojo) {
		GBResult result  = null;
		return cameraInfoService.rtspDevicePlay(pojo);
	}

	/**
	 * 测试向下级平台请求国标视频流
	 * @return
	 */
	@PostMapping(value = "testLowDevicePlay")
	public GBResult testLowDevicePlay(String deviceSerialNum, String channelId) throws Exception {
		Device device = new Device();
		Host host = new Host();
		host.setAddress("172.0.0.85:5060");
		host.setWanIp("172.0.0.85");
		host.setWanPort(5060);
		device.setHost(host);
		device.setProtocol("UDP");

		String streamName = StreamNameUtils.play(deviceSerialNum, channelId);
		// 4.2.1 创建推流地址
		String address = pushRtmpAddress.concat(streamName);
		// 4.2.2 创建callId
		String callId = null;
		callId = IDUtils.id();
		// 4.2.3 创建ssrc
		String ssrc = mSipLayer.getSsrc(true);
		// 4.2.4 创建端口号
		int port = mSipLayer.getPort(false);

		mSipLayer.sendInvite(device, SipLayer.SESSION_NAME_PLAY, callId, channelId, port, ssrc, false, "34020000002000000002", "172.0.0.85:15061");

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
		GBResult verifyResult = deviceManagerService.verifyDeviceInfo(specification, cameraInfo);
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
		GBResult verifyResult = deviceManagerService.verifyDeviceInfo(specification, cameraInfo);
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
		GBResult verifyResult = deviceManagerService.verifyDeviceInfo(specification, cameraInfo);
		int verifyCode = verifyResult.getCode();
		if (verifyCode != 200) {
			return verifyResult;
		}
		CameraPojo rtspPojo = (CameraPojo) verifyResult.getData();

		Integer psId = psConfig.getInteger("psId");
		String psName = psConfig.getString("psName");
		Integer positionChange = psConfig.getInteger("positionChange");

		// 2. 获取当前点位信息
		JSONObject presetPos = new JSONObject();
		if (positionChange == 1) {
			GBResult posResult = cameraControlService.getDVRConfig(specification, rtspPojo.getIp(), 8000, rtspPojo.getUsername(), rtspPojo.getPassword(), HCNetSDK.NET_DVR_SET_PTZPOS);
			JSONObject posJson = (JSONObject) posResult.getData();
			Integer pPos = posJson.getInteger("p");
			Integer tPos = posJson.getInteger("t");
			Integer zPos = posJson.getInteger("z");
			presetPos.put("pPos", pPos.shortValue());
			presetPos.put("tPos", tPos.shortValue());
			presetPos.put("zPos", zPos.shortValue());
		} else {
			// 2.1 如果不是改变现有的预置点点位，则获取旧的预置点位置
			PresetInfo presetInfo = presetInfoService.getById(psId);
			presetPos = JSONObject.parseObject(presetInfo.getPresetPos());
		}

		// 3. 插入或更新数据库
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
	@RequestMapping("delPTZPreset")
	public GBResult delPTZPreset(@RequestBody ControlCondition controlCondition) {
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
		GBResult verifyResult = deviceManagerService.verifyDeviceInfo(specification, cameraInfo);
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
		GBResult verifyResult = deviceManagerService.verifyDeviceInfo(specification, cameraInfo);
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
	 * ptz云台角度控制
	 * @param controlCondition
	 * @return
	 */
	@PostMapping("PTZAngleControl")
	public GBResult PTZAngleControl(@RequestBody ControlCondition controlCondition) {
		Integer deviceBaseId = controlCondition.getDeviceId();
		List<Integer> deviceIds = new ArrayList<>();
		JSONObject ptzPos = controlCondition.getPtzPos();
		deviceIds.add(deviceBaseId);

		Double p = ptzPos.getDouble("p");
		Double t = ptzPos.getDouble("t");
		Double z = ptzPos.getDouble("z");
		Integer parseP = CameraControlServiceImpl.DecToHexMa(p);
		Integer parseT = CameraControlServiceImpl.DecToHexMa(t);
		Integer parseZ = CameraControlServiceImpl.DecToHexMa(z);
		JSONObject settingJson = new JSONObject();
		settingJson.put("pPos", parseP);
		settingJson.put("tPos", parseT);
		settingJson.put("zPos", parseZ);

		DeviceBaseInfo deviceBaseInfo = deviceManagerService.getDeviceBaseInfoList(deviceIds).get(0);
		CameraInfo cameraInfo = deviceManagerService.getCameraInfoList(deviceIds).get(0);
		String specification = deviceBaseInfo.getSpecification();
		// 1. 验证设备信息
		GBResult verifyResult = deviceManagerService.verifyDeviceInfo(specification, cameraInfo);
		int verifyCode = verifyResult.getCode();
		if (verifyCode != 200) {
			return verifyResult;
		}
		CameraPojo rtspPojo = (CameraPojo) verifyResult.getData();

		// 2. 设置设备的ptz云台位置
		cameraControlService.setDVRConfig(specification, rtspPojo.getIp(), 8000, rtspPojo.getUsername(),
				rtspPojo.getPassword(), HCNetSDK.NET_DVR_SET_PTZPOS, settingJson);

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

			CacheUtil.deviceSnapshotMap.put(deviceBaseId, true);

			// 等待截图完成，并获取截图地址
			FileUtils.waitSnapshot(deviceBaseId, CacheUtil.deviceSnapshotMap.get(deviceBaseId));
			JSONObject snapshotAddressJson = CacheUtil.snapshotAddressMap.get(deviceBaseId);

			return GBResult.ok(snapshotAddressJson);
		}
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
