package com.yangjie.JGB28181.media.server.remux;

import com.alibaba.fastjson.JSONObject;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.yangjie.JGB28181.bean.WebSocketServer;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.RecordVideoInfo;
import com.yangjie.JGB28181.entity.SnapshotInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.service.SnapshotInfoService;
import com.yangjie.JGB28181.service.impl.CameraControlServiceImpl;
import com.yangjie.JGB28181.service.impl.RecordVideoInfoServiceImpl;
import com.yangjie.JGB28181.service.impl.SnapshotInfoServiceImpl;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.FrameRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Timer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;


/**
 * @Title CameraPush.java
 * @description 拉流推流
 * @time 2019年12月16日 上午9:34:41
 * @author wuguodong
 **/
@Component
public class RtspToRtmpPusher {

    private final static Logger logger = LoggerFactory.getLogger(RtspToRtmpPusher.class);

    // 配置类
    private static Config config = new Config();

    // 通过applicationContext上下文获取Config类
    public static void setApplicationContext(ApplicationContext applicationContext) {
//        logger.info("===============开始注入config=============");
//        config = applicationContext.getBean(Config.class);
//        System.out.println(config);
//        logger.info("===============完成注入config=============");
    }

    public static Timer timer;

    protected CustomFFmpegFrameGrabber grabber = null;// 解码器
    protected FFmpegFrameRecorder record = null;// 编码器
    protected FFmpegFrameRecorder record1 = null;
    int width;// 视频像素宽
    int height;// 视频像素高

    // 视频参数
    protected int audiocodecid;
    protected int codecid;
    protected double framerate;// 帧率
    protected int bitrate;// 比特率

    // 音频参数
    // 想要录制音频，这三个参数必须有：audioChannels > 0 && audioBitrate > 0 && sampleRate > 0
    private int audioChannels;
    private int audioBitrate;
    private int sampleRate;

    // 设备信息
    private CameraPojo cameraPojo;
    // 录像信息
    public RecordVideoInfo recordVideoInfo;
    // spring的上下文管理器
    private ApplicationContext applicationContext;
    // 录像文件
    public File file;
    private NativeLong lUserID = new NativeLong(0);
    private JSONObject resultJson = new JSONObject();
    private Long startTime = 0L;

    private CameraControlServiceImpl cameraControlService;
    private WebSocketServer webSocketServer;

    public RtspToRtmpPusher() {
        super();
    }

    public RtspToRtmpPusher(CameraPojo cameraPojo) {
        this.cameraPojo = cameraPojo;
        applicationContext = cameraPojo.getApplicationContext();
    }

    static {
        try {
            logger.info("===============开始注入config=============");
            File file = ResourceUtils.getFile("classpath:config.properties");
            InputStream in = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(in);
            String pushHost = properties.getProperty("config.push_host");
            String pushPort = properties.getProperty("config.push_port");
            String hostExtra = properties.getProperty("config.host_extra");
            String mainCode = properties.getProperty("config.main_code");
            String subCode = properties.getProperty("config.sub_code");

            config.setPush_port(pushPort);
            config.setPush_host(pushHost);
            config.setHost_extra(hostExtra);
            config.setMain_code(mainCode);
            config.setSub_code(subCode);

            System.out.println(config);
            logger.info("===============完成注入config=============");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 选择视频源
     *
     * @author wuguodong
     * @throws org.bytedeco.javacv.FrameGrabber.Exception
     * @throws org.bytedeco.javacv.FrameGrabber.Exception
     * @throws org.bytedeco.javacv.FrameGrabber.Exception
     * @throws Exception
     */
    public RtspToRtmpPusher from() throws Exception {
        // 采集/抓取器
        grabber = new CustomFFmpegFrameGrabber(cameraPojo.getRtsp());

        // 解决ip输入错误时，grabber.start();出现阻塞无法释放grabber而导致后续推流无法进行；
        Socket rtspSocket = new Socket();
        Socket rtmpSocket = new Socket();
        // 建立TCP Scoket连接，超时时间1s，如果成功继续执行，否则return
        logger.debug("******   TCPCheck    BEGIN   ******");
        try {
            rtspSocket.connect(new InetSocketAddress(cameraPojo.getIp(), 554), 1000);
        } catch (IOException e) {
            CacheUtil.failCidList.add(cameraPojo.getCid());
            grabber.stop();
            grabber.close();
            rtspSocket.close();
            logger.error("与拉流IP：   " + cameraPojo.getIp() + "   端口：   554    建立TCP连接失败！");
            return null;
        }
        try {
            rtmpSocket.connect(new InetSocketAddress(IpUtil.IpConvert(config.getPush_host()),
                    Integer.parseInt(config.getPush_port())), 1000);
        } catch (IOException e) {
            grabber.stop();
            grabber.close();
            rtspSocket.close();
            logger.error("与推流IP：   " + config.getPush_host() + "   端口：   " + config.getPush_port() + " 建立TCP连接失败！");
            return null;
        }

        logger.debug("******   TCPCheck    END     ******");

        if (cameraPojo.getRtsp().indexOf("rtsp") >= 0) {
            grabber.setOption("y", "");
            grabber.setOption("vsync", "0");
            // 使用硬件加速
            grabber.setOption("hwaccel", "cuvid");
            grabber.setVideoCodecName("h264_cuvid");
//            grabber.setVideoCodec(AV_CODEC_ID_H264);
            grabber.setOption("rtsp_transport", "tcp");// tcp用于解决丢包问题
            grabber.setOption("resize", "1920x1080");
        }

        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        FFmpegLogCallback.set();
        // 设置采集器构造超时时间
        grabber.setOption("stimeout", "2000000");
        try {
            logger.debug("******   grabber.start()    BEGIN   ******");

            if ("sub".equals(cameraPojo.getStream())) {
                grabber.start(config.getSub_code());
            } else if ("main".equals(cameraPojo.getStream())) {
                grabber.start(config.getMain_code());
            } else {
                grabber.start(config.getMain_code());
            }


            logger.debug("******   grabber.start()    END     ******");

            // 开始之后ffmpeg会采集视频信息，之后就可以获取音视频信息
            width = grabber.getImageWidth();
            height = grabber.getImageHeight();
            // 若视频像素值为0，说明拉流异常，程序结束
            if (width == 0 && height == 0) {
                logger.error(cameraPojo.getRtsp() + "  拉流异常！");
                grabber.stop();
                grabber.close();
                return null;
            }
            // 视频参数
            audiocodecid = grabber.getAudioCodec();
            codecid = grabber.getVideoCodec();
            framerate = grabber.getVideoFrameRate();// 帧率
            bitrate = grabber.getVideoBitrate();// 比特率
            // 音频参数
            // 想要录制音频，这三个参数必须有：audioChannels > 0 && audioBitrate > 0 && sampleRate > 0
            audioChannels = grabber.getAudioChannels();
            audioBitrate = grabber.getAudioBitrate();
            if (audioBitrate < 1) {
                audioBitrate = 128 * 1000;// 默认音频比特率
            }
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            logger.error("ffmpeg错误信息：", e);
            grabber.stop();
            grabber.close();
            return null;
        }

        return this;
    }

    /**
     * 选择输出
     *
     * @author wuguodong
     * @throws Exception
     */
    public RtspToRtmpPusher to() throws Exception {
        // 录制/推流器
        if (cameraPojo.getToHls() == 1) {
            record = new FFmpegFrameRecorder(cameraPojo.getHls(), 1280, 720);
        } else {
            record = new FFmpegFrameRecorder(cameraPojo.getRtmp(), 1280, 720);
        }

        this.setPushRecorderOption();
        AVFormatContext fc = null;
        if (cameraPojo.getRtmp().indexOf("rtmp") >= 0 || cameraPojo.getRtmp().indexOf("flv") > 0) {
            // 封装格式flv
            record.setFormat("flv");
            record.setAudioCodecName("aac");
        }
        fc = grabber.getFormatContext();
        try {
            record.start(fc);
            startTime = System.currentTimeMillis();
        } catch (Exception e) {
            logger.error(cameraPojo.getRtsp() + "  推流异常！");
            logger.error("ffmpeg错误信息：", e);
            grabber.stop();
            grabber.close();
            record.stop();
            record.close();
            return null;
        }
        return this;

    }

    /**
     * 转封装
     *
     * @author wuguodong
     * @throws org.bytedeco.javacv.FrameGrabber.Exception
     * @throws org.bytedeco.javacv.FrameRecorder.Exception
     * @throws InterruptedException
     */
    public RtspToRtmpPusher go(Thread nowThread)
            throws org.bytedeco.javacv.FrameGrabber.Exception, org.bytedeco.javacv.FrameRecorder.Exception {
        long err_index = 0;// 采集或推流导致的错误次数
        // 连续五次没有采集到帧则认为视频采集结束，程序错误次数超过5次即中断程序
        logger.info(cameraPojo.getRtsp() + " 开始推流...");
        // 释放探测时缓存下来的数据帧，避免pts初始值不为0导致画面延时
        grabber.flush();
        int isTest = cameraPojo.getIsTest();
        file = new File(cameraPojo.getRecordDir());
        cameraControlService = (CameraControlServiceImpl) applicationContext.getBean("cameraControlServiceImpl");
        webSocketServer = (WebSocketServer) applicationContext.getBean("webSocketServer");

        CacheUtil.deviceStreamingMap.put(Integer.valueOf(cameraPojo.getDeviceId()), true);

        // 获取PTZ云台的位置坐标
//        ActionController.executor.execute(() -> {
//            getPTZPosition();
//        });

        for (int no_frame_index = 0; no_frame_index < 5 || err_index < 5;) {
            try {
                // 用于中断线程时，结束该循环
                nowThread.sleep(0);
                AVPacket packet;
                packet = grabber.grabPacket();
                // 数据为空时跳过
                if (null == packet && packet.size() == 0) {
                    continue;
                }

                // 如果是测试推流则直接跳出
                if (isTest == 1) {
                    break;
                }

                record.recordPacket(packet);

                // 检测推流信条
                String token = cameraPojo.getToken();
                Long heartbeats = CacheUtil.heartbeatsMap.get(token);
                if (null != heartbeats) {
                    heartbeats++;
                } else {
                    heartbeats = 1L;
                }
                CacheUtil.heartbeatsMap.put(token, heartbeats);
            } catch (InterruptedException e) {
                CacheUtil.deviceStreamingMap.put(Integer.valueOf(cameraPojo.getDeviceId()), false);
                e.printStackTrace();
                // 销毁构造器
                grabber.stop();
                grabber.close();
                record.stop();
                record.close();
                logger.info(cameraPojo.getRtsp() + " 中断推流成功！");
                break;
            }
        }
        CacheUtil.deviceStreamingMap.put(Integer.valueOf(cameraPojo.getDeviceId()), false);
        // 程序正常结束销毁构造器
        grabber.stop();
        grabber.close();
        record.stop();
        record.close();
        logger.info(cameraPojo.getRtsp() + " 推流结束...");
        return this;
    }

    private void getPTZPosition() {
        HCNetSDK hcNetSDK = HCNetSDK.INSTANCE;

        // 1.初始化sdk
        boolean initSuc = hcNetSDK.NET_DVR_Init();
        if (!initSuc) {
            logger.info("初始化sdk失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
        }
        // 2.登录设备
        if (lUserID.intValue() <= 0) {
            lUserID = hcNetSDK.NET_DVR_Login_V30(cameraPojo.getIp(), (short) 8000, cameraPojo.getUsername(), cameraPojo.getPassword(), null);//登陆
            if (lUserID.intValue() < 0) {
                logger.info("登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
            }
        }

        while (true) {
            Long useTime = System.currentTimeMillis() - startTime;
            // 3.创建PTZPOS参数对象
            HCNetSDK.NET_DVR_PTZPOS net_dvr_ptzpos = new HCNetSDK.NET_DVR_PTZPOS();
            Pointer pos = net_dvr_ptzpos.getPointer();

            // 4.获取PTZPOS参数
            hcNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_PTZPOS, new NativeLong(1), pos, net_dvr_ptzpos.size(), new IntByReference(0));
            net_dvr_ptzpos.read();

            // 5.放入到临时缓存中
            resultJson.put("deviceBaseId", cameraPojo.getDeviceId());
            resultJson.put("p", net_dvr_ptzpos.wPanPos);
            resultJson.put("t", net_dvr_ptzpos.wTiltPos);
            resultJson.put("z", net_dvr_ptzpos.wZoomPos);
            resultJson.put("timestamp", useTime);
        }
    }

    /**
     * 设置推流推流器参数
     */
    private void setPushRecorderOption() {
        String streamSize = DeviceManagerController.cameraConfigBo.getStreamSize();
        Integer streamWidth = Integer.valueOf(streamSize.split("x")[0]);
        Integer streamHeight = Integer.valueOf(streamSize.split("x")[1]);

        record.setFrameRate(framerate);
        record.setVideoCodec(AV_CODEC_ID_H264);
        record.setImageHeight(streamHeight);
        record.setImageWidth(streamWidth);
        record.setVideoBitrate(Integer.valueOf(DeviceManagerController.cameraConfigBo.getStreamMaxRate()));

        // 设置分片时间
        if (cameraPojo.getToHls() == 1) {
            record.setOption("hls_time", "10");
        }

        if (cameraPojo.getRtmp().indexOf("rtmp") >= 0 || cameraPojo.getRtmp().indexOf("flv") > 0) {
            // 封装格式flv
            record.setFormat("flv");
            record.setAudioCodecName("aac");
        }
    }

    /**
     * 设置录像推流器参数
     */
    private void setRecordRecorderOption() {
        String recordSize = DeviceManagerController.cameraConfigBo.getRecordSize();
        Integer recordWidth = Integer.valueOf(recordSize.split("x")[0]);
        Integer recordHeight = Integer.valueOf(recordSize.split("x")[1]);

        record1.setFrameRate(framerate);
        record1.setVideoCodec(AV_CODEC_ID_H264);
        record1.setImageHeight(recordHeight);
        record1.setImageWidth(recordWidth);
        record1.setVideoBitrate(Integer.valueOf(DeviceManagerController.cameraConfigBo.getRecordMaxRate()));
        record1.setFormat("flv");
    }

    /**
     * 在数据库新建录像文件的信息
     * @param recordVideoInfo
     */
    private void saveRecordFileInfo(RecordVideoInfo recordVideoInfo) {
        this.recordVideoInfo = recordVideoInfo;
        if (null == recordVideoInfo.getDeviceBaseId()) {
            this.recordVideoInfo.setDeviceBaseId(Integer.valueOf(cameraPojo.getDeviceId()));
        }
        if (null == recordVideoInfo.getStartTime()) {
            this.recordVideoInfo.setStartTime(LocalDateTime.now());
        }
        if (null == recordVideoInfo.getFilePath()) {
            this.recordVideoInfo.setFilePath(cameraPojo.getRecordDir());
        }
        RecordVideoInfoServiceImpl recordVideoInfoService = (RecordVideoInfoServiceImpl) applicationContext.getBean("recordVideoInfoServiceImpl");
        recordVideoInfoService.saveOrUpdate(this.recordVideoInfo);
    }

    /**
     * 超过单个文件最长时间则进行重新录像
     * @throws FrameRecorder.Exception
     */
    private void restartRecorderWithMaxTime() throws FrameRecorder.Exception {
        long timestamp = record1.getTimestamp();
        if (timestamp > Long.valueOf(DeviceManagerController.cameraConfigBo.getRecordInterval())) {
            record1.stop();

            recordVideoInfo.setEndTime(LocalDateTime.now());
            recordVideoInfo.setFileSize(file.length());
            // 更新原有的录像文件信息
            this.saveRecordFileInfo(recordVideoInfo);

            String address = RecordNameUtils.recordVideoFileAddress(StreamNameUtils.rtspPlay(cameraPojo.getDeviceId(), "1"));
            file = new File(address);
            record1 = new FFmpegFrameRecorder(address, 1280, 720);
            this.setRecordRecorderOption();
            record1.start();

            // 新建一条录像文件信息
            this.saveRecordFileInfo(new RecordVideoInfo());
        }
    }

    /**
     * 超过单个文件最大大小则进行重新录像
     * @throws FrameRecorder.Exception
     */
    private void restartRecorderWithMaxSize() throws FrameRecorder.Exception {
        if (file.length() > Long.valueOf(DeviceManagerController.cameraConfigBo.getRecordStSize())) {
            record1.stop();

            recordVideoInfo.setEndTime(LocalDateTime.now());
            recordVideoInfo.setFileSize(file.length());
            // 更新原有的录像文件信息
            this.saveRecordFileInfo(recordVideoInfo);

            String address = RecordNameUtils.recordVideoFileAddress(StreamNameUtils.rtspPlay(cameraPojo.getDeviceId(), "1"));
            file = new File(address);
            record1 = new FFmpegFrameRecorder(address, 1280, 720);
            this.setRecordRecorderOption();
            record1.start();

            // 新建一条录像文件信息
            this.saveRecordFileInfo(new RecordVideoInfo());
        }
    }
}