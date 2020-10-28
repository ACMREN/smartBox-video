package com.yangjie.JGB28181.web.controller;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.sip.Dialog;
import javax.sip.SipException;
import javax.xml.namespace.QName;

import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.media.server.handler.TCPHandler;
import com.yangjie.JGB28181.service.impl.PushHlsStreamServiceImpl;
import org.apache.cxf.ws.discovery.WSDiscoveryClient;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;
import org.eclipse.jetty.util.BlockingArrayQueue;
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
import com.yangjie.JGB28181.common.utils.IDUtils;
import com.yangjie.JGB28181.common.utils.RedisUtil;
import com.yangjie.JGB28181.common.utils.StreamNameUtils;
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

import static com.yangjie.JGB28181.common.constants.ResultConstants.*;


@RestController
@RequestMapping("/camera/")
@EnableConfigurationProperties(ConfigProperties.class)
public class ActionController implements OnProcessListener {

	@Autowired
	private SipLayer mSipLayer;

	@Autowired
	private PushHlsStreamServiceImpl pushHlsStreamService;

	private MessageManager mMessageManager = MessageManager.getInstance();

	public static PushStreamDeviceManager mPushStreamDeviceManager = PushStreamDeviceManager.getInstance();

	@Value("${config.pullRtmpAddress}")
	private String pullRtmpAddress;


	@Value("${config.pushRtmpAddress}")
	private String pushRtmpAddress;

	@Value("${config.checkSsrc}")
	private boolean checkSsrc;

	public static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

	public static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 3000, TimeUnit.MILLISECONDS, new BlockingArrayQueue<>(10));

	public static Map<String, JSONObject> streamRelationMap = new HashMap<>(20);

	public static Map<String, TCPHandler> tcpHandlerMap = new HashMap<>(20);

	/**
	 * 播放rtmp基础视频流
	 * @param deviceId 设备id
	 * @param channelId 通道id
	 * @param mediaProtocol 推流协议，默认为tcp
	 * @return
	 */
	@RequestMapping("play")
	public GBResult play(
			@RequestParam("deviceId")String deviceId,
			@RequestParam("channelId")String channelId,
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
			callId = "abc";
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
				observer.startRemux();

				observer.setOnProcessListener(this);
				mPushStreamDeviceManager.put(streamName, callId, Integer.valueOf(ssrc), pushStreamDevice);
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
		Set<String> deviceUrlSet = new HashSet<>();
		WSDiscoveryClient client = new WSDiscoveryClient();
		client.setVersion10();
		client.setDefaultProbeTimeout(5000);
		ProbeType probeType = new ProbeType();
		probeType.getTypes().add(new QName("tds:Device"));
		probeType.getTypes().add(new QName("dn:Network Video Transmitter"));
		try	{
			ProbeMatchesType probeMatchesType = client.probe(probeType);
			List<ProbeMatchType> probeMatchTypeList = probeMatchesType.getProbeMatch();
			for (ProbeMatchType type : probeMatchTypeList) {
				List<String> xAddrs = type.getXAddrs();
				for (String XAddr : xAddrs) {
					if (XAddr.contains("onvif/device_service")) {
						deviceUrlSet.add(XAddr);
					}
				}
			}
			return GBResult.ok(deviceUrlSet);
		} catch (Exception e) {
			return GBResult.build(NOT_FOUND_DEVICE_CODE, NOT_FOUND_DEVICE);
		}
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
