package com.yangjie.JGB28181.common.utils;


import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.PushStreamDevice;
import com.yangjie.JGB28181.bean.RecordStreamDevice;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.media.server.Server;
import com.yangjie.JGB28181.media.server.handler.GBStreamHandler;
import com.yangjie.JGB28181.media.server.remux.Observer;
import com.yangjie.JGB28181.media.server.remux.RtspRecorder;
import com.yangjie.JGB28181.media.server.remux.RtspToRtmpPusher;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @Title CacheUtil.java
 * @description 推流缓存信息
 * @time 2019年12月17日 下午3:12:45
 * @author wuguodong
 **/
public final class CacheUtil {
	/*
	 * 保存已经开始推的流
	 */
	public static Map<String, CameraPojo> STREAMMAP = new HashMap<String, CameraPojo>();

	public static Map<String, CameraPojo> rtspVideoRecordMap= new HashMap<>();

	/*
	 * 保存服务启动时间
	 */
	public static long STARTTIME;


	// 关闭推流的标志位
	public static volatile Map<String, Boolean> callEndMap = new ConcurrentHashMap<>(20);

	// 定时器执行线程池
	public static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

	public static Map<Integer, ScheduledFuture> scheduledFutureMap = new HashMap<>();

	public static ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 3000, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>(10));

	public static Map<String, Integer> callIdCountMap = new HashMap<>(20);

	// 判断设备是否正在推流
	public static Map<Integer, PushStreamDevice> streamingDeviceMap = new HashMap<>(20);

	// 保存设备基础id与推流id的关系
	public static Map<Integer, JSONObject> baseDeviceIdCallIdMap = new HashMap<>(20);

	// 存放任务 线程
	public static Map<String, CameraThread.MyRunnable> jobMap = new HashMap<String, CameraThread.MyRunnable>();

	// rtsp设备的线程处理器map
	public static Map<Integer, RtspToRtmpPusher> rtspPusherMap = new HashMap<>(20);

	// rtsp设备的录像处理器map
	public static Map<Integer, RtspRecorder> rtspRecorderMap = new HashMap<>(20);

	// 国标设备录像map
	public static Map<Integer, RecordStreamDevice> deviceRecordMap = new HashMap<>(20);

	// 国标设备服务器map
	public static Map<String, Observer> gbPushObserver = new HashMap<>(20);

	public static Map<String, Server> gbServerMap = new HashMap<>(20);

	// 失败设备id列表
	public static List<Integer> failCidList = new ArrayList<>(20);

	// 截图标志位map
	public static Map<Integer, Boolean> deviceSnapshotMap = new HashMap<>(20);

	// 截图地址map
	public static Map<Integer, JSONObject> snapshotAddressMap = new HashMap<>();

	public static Map<Integer, Boolean> deviceStreamingMap = new HashMap<>(20);

	public static Map<Integer, Boolean> deviceRecordingMap = new HashMap<>(20);

	public static Map<String, GBStreamHandler> deviceHandlerMap = new HashMap<>(20);

}
