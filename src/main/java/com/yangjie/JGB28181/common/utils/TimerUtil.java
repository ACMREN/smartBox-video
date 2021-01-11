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
								CacheUtil.jobMap.get(key).setInterrupted();
								// 清除缓存
								CacheUtil.STREAMMAP.remove(key);
								CacheUtil.jobMap.remove(key);
							}

							// 如果推流停止了，则停止推流，要重新获取
							Long heartbeats = heartbeatsMap.get(key);
							Long lastHeartbeats = lastHeartbeatsMap.get(key);
							if (lastHeartbeats != null && lastHeartbeats - heartbeats == 0) {
								int isTest = cameraPojo.getIsTest();
								logger.info("移除已经停止推流的直播，key：" + key);
								CacheUtil.STREAMMAP.remove(key);
								CacheUtil.jobMap.remove(key);
								if (isTest != 1) {
									cameraInfoService.openStream(cameraPojo);
								}
							}
							lastHeartbeatsMap.put(key, heartbeats);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				// 关闭超过5分钟没有人观看的推流
				Map<Integer, JSONObject> baseDeviceIdCallIdMap = CacheUtil.baseDeviceIdCallIdMap;
				for (Integer deviceBaseId : baseDeviceIdCallIdMap.keySet()) {
					JSONObject streamJson = baseDeviceIdCallIdMap.get(deviceBaseId);
					for (int i = 0; i < 2; i++) {
						String streamType = null;
						if (i == 0) {
							streamType = BaseConstants.PUSH_STREAM_RTMP;
						}
						if (i == 1) {
							streamType  = BaseConstants.PUSH_STREAM_HLS;
						}
						JSONObject typeStreamJson = streamJson.getJSONObject(streamType);
						if (null == typeStreamJson) {
							continue;
						}
						String callId = typeStreamJson.getString("callId");
						String str = RedisUtil.get(callId);
						CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceBaseId);
						if (StringUtils.isEmpty(str)) {
							Integer linkType = cameraInfo.getLinkType();
							// 如果是rtmp推流，则有区分国标和rtsp链接
							if (BaseConstants.PUSH_STREAM_RTMP.equals(streamType)) {
								if (LinkTypeEnum.RTSP.getCode() == linkType.intValue()) {
									CacheUtil.jobMap.get(callId).setInterrupted();
								}
								if (LinkTypeEnum.GB28181.getCode() == linkType.intValue()) {
									try {
										sipLayer.sendBye(callId);
									} catch (SipException e) {
										e.printStackTrace();
									}
								}
							}
							// 关闭hls推流
							if (BaseConstants.PUSH_STREAM_HLS.equals(streamType)) {
								if (LinkTypeEnum.RTSP.getCode() == linkType.intValue()) {
									CacheUtil.jobMap.get(callId).setInterrupted();
								}
								if (LinkTypeEnum.GB28181.getCode() == linkType.intValue()) {
									try {
										sipLayer.sendBye(callId);
									} catch (SipException e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
				}

				logger.info("******   执行定时任务       END     ******");
			}
		}, 1, 1000 * 30);
	}
}
