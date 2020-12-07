package com.yangjie.JGB28181.common.thread;

import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.common.utils.IpUtil;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.media.server.remux.RtspToRtmpPusher;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                CacheUtil.STREAMMAP.put(cameraPojo.getToken(), cameraPojo);
                // 执行转流推流任务
                Integer deviceId  = Integer.valueOf(cameraPojo.getDeviceId());
                RtspToRtmpPusher push;
                if (null != ActionController.rtspPusherMap.get(deviceId)) {
                    push = ActionController.rtspPusherMap.get(deviceId);
                } else {
                    push = new RtspToRtmpPusher(cameraPojo).from();
                }
                if (push != null) {
                    push.to().go(nowThread);
                }
                // 清除缓存
                CacheUtil.STREAMMAP.remove(cameraPojo.getToken());
                ActionController.jobMap.remove(cameraPojo.getToken());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("当前任务： " + cameraPojo.getRtsp() + "停止...");
            }
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
                rtmp = "rtmp://" + IpUtil.IpConvert(this.push_host) + ":" + this.push_host + "/history/"
                        + token;
                if (this.host_extra.equals("127.0.0.1")) {
                    url = rtmp;
                } else {
                    url = "rtmp://" + IpUtil.IpConvert(this.host_extra) + ":" + this.push_host + "/history/"
                            + token;
                }
            } else {// 直播流
                rtsp = "rtsp://" + username + ":" + password + "@" + IP + ":554/h264/ch" + channel + "/" + stream
                        + "/av_stream";
                rtmp = "rtmp://" + IpUtil.IpConvert(this.push_host) + ":" + this.push_host + "/live/" + token;
                if (this.host_extra.equals("127.0.0.1")) {
                    url = rtmp;
                } else {
                    url = "rtmp://" + IpUtil.IpConvert(this.host_extra) + ":" + this.push_host + "/live/"
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
}
