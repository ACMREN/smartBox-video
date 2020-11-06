package com.yangjie.JGB28181.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.message.SipLayer;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.impl.PushHlsStreamServiceImpl;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sip.SipException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Title TimerUtil.java
 * @description 定时任务
 * @time 2019年12月16日 下午3:10:08
 * @author wuguodong
 **/
@Component
public class TimerUtil implements CommandLineRunner {

	private final static Logger logger = LoggerFactory.getLogger(TimerUtil.class);

	@Autowired
	private Config config;// 配置文件bean

	@Autowired
	SipLayer sipLayer;

	@Autowired
	private CameraInfoService cameraInfoService;

	@Autowired
	private PushHlsStreamServiceImpl pushHlsStreamService;

	public static Timer timer;

	public static Map<String, Long> heartbeatsMap = new HashMap<>();

	public static Map<String, Long> lastHeartbeatsMap = new HashMap<>();

	@Override
	public void run(String... args) throws Exception {
		// 超过5分钟，结束推流
		timer = new Timer("timeTimer");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				logger.info("******   执行定时任务       BEGIN   ******");
				// 管理缓存
				if (null != CacheUtil.STREAMMAP && 0 != CacheUtil.STREAMMAP.size()) {
					Set<String> keys = CacheUtil.STREAMMAP.keySet();
					for (String key : keys) {
                        CameraPojo cameraPojo = CacheUtil.STREAMMAP.get(key);
                        try {
							// 如果通道使用人数为0，则关闭推流
							if (CacheUtil.STREAMMAP.get(key).getCount() == 0) {
								// 结束线程
								ActionController.jobMap.get(key).setInterrupted();
								// 清除缓存
								CacheUtil.STREAMMAP.remove(key);
								ActionController.jobMap.remove(key);
							}

							// 如果推流停止了，则停止推流，要重新获取
							Long heartbeats = heartbeatsMap.get(key);
							Long lastHeartbeats = lastHeartbeatsMap.get(key);
							if (lastHeartbeats != null && lastHeartbeats - heartbeats == 0) {
								logger.info("移除已经停止推流的直播，key：" + key);
								CacheUtil.STREAMMAP.remove(key);
								ActionController.jobMap.remove(key);
								openStream(cameraPojo.getIp(), cameraPojo.getUsername(), cameraPojo.getPassword(), cameraPojo.getChannel(), cameraPojo.getStream(), cameraPojo.getStartTime(),
										cameraPojo.getEndTime(), cameraPojo.getOpenTime());
							}
							lastHeartbeatsMap.put(key, heartbeats);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				Map<Integer, JSONObject> baseDeviceIdCallIdMap = ActionController.baseDeviceIdCallIdMap;
				for (Integer deviceBaseId : baseDeviceIdCallIdMap.keySet()) {
					JSONObject streamJson = baseDeviceIdCallIdMap.get(deviceBaseId);
					String callId = streamJson.getString("callId");
					String streamType = streamJson.getString("type");
					String str = RedisUtil.get(callId);
					CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceBaseId);
					if (StringUtils.isEmpty(str)) {
						Integer linkType = cameraInfo.getLinkType();
						// 如果是rtmp推流，则有区分国标和rtsp链接
						if (BaseConstants.PUSH_STREAM_RTMP.equals(streamType)) {
							if (LinkTypeEnum.RTSP.getCode() == linkType.intValue()) {
								ActionController.jobMap.get(callId).setInterrupted();
							}
							if (LinkTypeEnum.GB28181.getCode() == linkType.intValue()) {
								try {
									sipLayer.sendBye(callId);
								} catch (SipException e) {
									e.printStackTrace();
								}
							}
						}
						// hls推流统一都是都是通过命令行执行，无需区分
						if (BaseConstants.PUSH_STREAM_HLS.equals(streamType)) {
							pushHlsStreamService.closeStream(callId);
						}
					}
				}
				logger.info("******   执行定时任务       END     ******");
			}
		}, 1, 1000 * 30);
	}

	/**
	 * @Title: openStream
	 * @Description: 推流器
	 * @param ip
	 * @param username
	 * @param password
	 * @param channel
	 * @param stream
	 * @param starttime
	 * @param endtime
	 * @param openTime
	 * @return
	 * @return CameraPojo
	 **/
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
		if (null != starttime && !"".equals(starttime)) {
			if (null != endtime && !"".equals(endtime)) {
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
		ActionController.jobMap.put(token, job);

		return cameraPojo;
	}
}
