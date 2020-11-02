package com.yangjie.JGB28181.web.controller;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sip.Dialog;
import javax.sip.SipException;

import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.media.server.handler.TCPHandler;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.service.impl.PushHlsStreamServiceImpl;
import org.eclipse.jetty.util.BlockingArrayQueue;
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

	private MessageManager mMessageManager = MessageManager.getInstance();

	public static PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance();

	@Value("${config.pushHlsAddress}")
	private String pushHlsAddress;


	@Value("${config.pushRtmpAddress}")
	private String pushRtmpAddress;

	@Value("${config.checkSsrc}")
	private boolean checkSsrc;

	public static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

	public static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 3000, TimeUnit.MILLISECONDS, new BlockingArrayQueue<>(10));

	public static Map<String, JSONObject> streamRelationMap = new HashMap<>(20);

	public static Map<String, TCPHandler> tcpHandlerMap = new HashMap<>(20);

	public static Map<Integer, PushStreamDevice> streamingDeviceMap = new HashMap<>(20);

	// 存放任务 线程
	public static Map<String, CameraThread.MyRunnable> jobMap = new HashMap<String, CameraThread.MyRunnable>();



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
			@RequestParam(required = false,name = "protocol",defaultValue = "TCP")String
			mediaProtocol){
		GBResult result = null;
		try{
			int pushPort = 1935;
			//1.从redis查找设备，如果不存在，返回离线
			String deviceStr = RedisUtil.get(deviceId);
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
				String address = pushHlsAddress.concat(streamName);
				Server server = isTcp ? new TCPServer() : new UDPServer();
				Observer observer = new RtmpPusher(address, callId);
				((RtmpPusher) observer).setDeviceId(streamName);
				
				server.subscribe(observer);
				pushStreamDevice = new PushStreamDevice(deviceId,Integer.valueOf(ssrc),callId,streamName,port,isTcp,server,
						observer,address);
				
				pushStreamDevice.setDialog(response);
				server.startServer(pushStreamDevice.getFrameDeque(),Integer.valueOf(ssrc),port,false, streamName);
				observer.startRemux();

				observer.setOnProcessListener(this);
				mPushStreamDeviceManager.put(streamName, callId, Integer.valueOf(ssrc), pushStreamDevice);
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
	public Map<String, String> openCamera(CameraPojo pojo) {
		// 返回结果
		Map<String, String> map = new HashMap<String, String>();
		// 校验参数
		if (StringUtils.isEmpty(pojo.getIp()) && StringUtils.isEmpty(pojo.getUsername()) && StringUtils.isEmpty(pojo.getPassword())
				&& StringUtils.isEmpty(pojo.getChannel())) {
			CameraPojo cameraPojo = new CameraPojo();
			// 获取当前时间
			String openTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime());
			Set<String> keys = CacheUtil.STREAMMAP.keySet();
			// 缓存是否为空
			if (0 == keys.size()) {
				// 开始推流
				cameraPojo = openStream(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(),
						pojo.getStream(), pojo.getStartTime(), pojo.getEndTime(), openTime);
				map.put("token", cameraPojo.getToken());
				map.put("url", cameraPojo.getUrl());
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
						map.put("token", cameraPojo.getToken());
						map.put("url", cameraPojo.getUrl());
						logger.info("打开：" + cameraPojo.getRtsp());
					} else {
						cameraPojo = openStream(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(),
								pojo.getStream(), pojo.getStartTime(), pojo.getEndTime(), openTime);
						map.put("token", cameraPojo.getToken());
						map.put("url", cameraPojo.getUrl());
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
								pojo.getStream(), pojo.getStartTime(), pojo.getEndTime(), openTime);
						map.put("token", cameraPojo.getToken());
						map.put("url", cameraPojo.getUrl());
						logger.info("打开：" + cameraPojo.getRtsp());
					}
				}
			}
		}
		return map;
	}

	public CameraPojo openStream(String ip, String username, String password, String channel, String stream,
								 String starttime, String endtime, String openTime) {
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
		if (StringUtils.isEmpty(starttime)) {
			if (StringUtils.isEmpty(endtime)) {
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

		// 执行任务
		CameraThread.MyRunnable job = new CameraThread.MyRunnable(cameraPojo);
		CameraThread.MyRunnable.es.execute(job);
		jobMap.put(token, job);

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
	@RequestMapping(value = "/playHls")
	public GBResult toHls(@RequestParam(value = "deviceId")String deviceId,
						  @RequestParam(value = "channelId")String channelId) {
		Boolean isAlive = PushHlsStreamServiceImpl.deviceInfoMap.get(deviceId);
		if (isAlive != null && isAlive) {
			return GBResult.ok();
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
	 * 关闭hls视频流
	 * @param callId
	 * @return
	 */
	@RequestMapping(value = "closeHls")
	public GBResult closeHls(@RequestParam(value = "callId")String callId) {
		Process hlsProcess = PushHlsStreamServiceImpl.hlsProcessMap.get(callId);
		if (hlsProcess.isAlive()) {
			hlsProcess.destroy();

			// 删除文件夹及其内容
			JSONObject hlsInfoJSon = PushHlsStreamServiceImpl.hlsInfoMap.get(callId);
			String deviceId = hlsInfoJSon.getString("deviceId");
			String channelId = hlsInfoJSon.getString("channelId");
			String playFileName = StreamNameUtils.play(deviceId, channelId);
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
		}

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
