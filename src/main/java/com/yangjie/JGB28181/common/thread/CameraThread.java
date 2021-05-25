package com.yangjie.JGB28181.common.thread;

import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.common.utils.IpUtil;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.media.server.remux.RtspRecorder;
import com.yangjie.JGB28181.media.server.remux.RtspToRtmpPusher;
import com.yangjie.JGB28181.service.impl.CameraInfoServiceImpl;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraThread {

    private final static Logger logger = LoggerFactory.getLogger(CameraThread.class);

    public static class MyRunnable implements Runnable {

        private String push_host = "127.0.0.1";
        private String host_extra = "127.0.0.1";
        private String push_port = "1935";

        // 创建线程池
        public static ExecutorService es = Executors.newCachedThreadPool();

        private CameraPojo cameraPojo;
        private Thread nowThread;

        public MyRunnable(CameraPojo cameraPojo) {
            this.cameraPojo = cameraPojo;
        }

        // 中断线程
        public void setInterrupted() {
            nowThread.interrupt();
        }

        @Override
        public void run() {
            // 直播流
            try {
                // 获取当前线程存入缓存
                nowThread = Thread.currentThread();
                if (cameraPojo.getIsRecord() == 0) {
                    CacheUtil.STREAMMAP.put(cameraPojo.getToken(), cameraPojo);
                } else {
                    CacheUtil.rtspVideoRecordMap.put(cameraPojo.getToken(), cameraPojo);
                }
                // 执行转流推流任务
                Integer deviceId  = Integer.valueOf(cameraPojo.getDeviceId());
                RtspToRtmpPusher push;
                RtspRecorder recorder;
                // 如果是推流的话，就创建推流处理器
                if (cameraPojo.getIsRecord() == 0) {
                    if (null != CacheUtil.rtspPusherMap.get(deviceId)) {
                        push = CacheUtil.rtspPusherMap.get(deviceId);
                    } else {
                        push = new RtspToRtmpPusher(cameraPojo).from();
                    }
                    if (push != null) {
                        push.to().go(nowThread);
                    }
                }
                // 如果是录像的话，就创建录像处理器
                if (cameraPojo.getIsRecord() == 1) {
                    if (null != CacheUtil.rtspRecorderMap.get(deviceId)) {
                        recorder = CacheUtil.rtspRecorderMap.get(deviceId);
                    } else {
                        recorder = new RtspRecorder(cameraPojo).from();
                    }
                    if (recorder != null) {
                        recorder.to().go(nowThread);
                    }
                }
                // 清除缓存
                CacheUtil.STREAMMAP.remove(cameraPojo.getToken());
                CacheUtil.rtspVideoRecordMap.remove(cameraPojo.getToken());
                CacheUtil.jobMap.remove(cameraPojo.getToken());
            } catch (Exception e) {
                // 清除缓存
                CacheUtil.STREAMMAP.remove(cameraPojo.getToken());
                CacheUtil.rtspVideoRecordMap.remove(cameraPojo.getToken());
                CacheUtil.jobMap.remove(cameraPojo.getToken());
                // 重启推流/录像
                ApplicationContext applicationContext = cameraPojo.getApplicationContext();
                CameraInfoServiceImpl cameraInfoServiceImpl = (CameraInfoServiceImpl) applicationContext.getBean("cameraInfoServiceImpl");
                cameraInfoServiceImpl.openStream(cameraPojo);
                e.printStackTrace();
                logger.error("当前任务： " + cameraPojo.getRtsp() + "停止...");
            }
        }
    }
}
