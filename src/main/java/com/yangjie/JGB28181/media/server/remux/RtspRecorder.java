package com.yangjie.JGB28181.media.server.remux;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.WebSocketServer;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.*;
import com.yangjie.JGB28181.entity.RecordVideoInfo;
import com.yangjie.JGB28181.entity.SnapshotInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.service.impl.CameraControlServiceImpl;
import com.yangjie.JGB28181.service.impl.RecordVideoInfoServiceImpl;
import com.yangjie.JGB28181.service.impl.SnapshotInfoServiceImpl;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
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

public class RtspRecorder {
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

    private CameraControlServiceImpl cameraControlService;
    private WebSocketServer webSocketServer;

    public RtspRecorder() {
        super();
    }

    public RtspRecorder(CameraPojo cameraPojo) {
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
    public RtspRecorder from() throws Exception {
        // 采集/抓取器
        if (null == grabber) {
            grabber = new CustomFFmpegFrameGrabber(cameraPojo.getRtsp());
            ActionController.rtspDeviceGrabberMap.put(Integer.valueOf(cameraPojo.getDeviceId()), grabber);
        } else {
            grabber = (CustomFFmpegFrameGrabber) ActionController.rtspDeviceGrabberMap.get(Integer.valueOf(cameraPojo.getDeviceId()));
        }

        // 解决ip输入错误时，grabber.start();出现阻塞无法释放grabber而导致后续推流无法进行；
        Socket rtspSocket = new Socket();
        Socket rtmpSocket = new Socket();
        // 建立TCP Scoket连接，超时时间1s，如果成功继续执行，否则return
        logger.debug("******   TCPCheck    BEGIN   ******");
        try {
            rtspSocket.connect(new InetSocketAddress(cameraPojo.getIp(), 554), 1000);
        } catch (IOException e) {
            ActionController.failCidList.add(cameraPojo.getCid());
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
                grabber.start(config.getSub_code());
            }
            ActionController.rtspDeviceGrabberMap.put(Integer.valueOf(cameraPojo.getDeviceId()), grabber);


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
    public RtspRecorder to() throws Exception {
        // 录制/推流器
        record1 = new FFmpegFrameRecorder(cameraPojo.getRecordDir(), 1280, 720, 0);

        this.setRecordRecorderOption();
        AVFormatContext fc = null;
        try {
            record1.start();
            // 在数据库新建录像文件的信息
            this.saveRecordFileInfo(new RecordVideoInfo());
        } catch (Exception e) {
            logger.error(cameraPojo.getRtsp() + "  推流异常！");
            logger.error("ffmpeg错误信息：", e);
            grabber.stop();
            grabber.close();
            record1.stop();
            record1.close();
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
    public RtspRecorder go(Thread nowThread)
            throws org.bytedeco.javacv.FrameGrabber.Exception, org.bytedeco.javacv.FrameRecorder.Exception {
        long err_index = 0;// 采集或推流导致的错误次数
        // 连续五次没有采集到帧则认为视频采集结束，程序错误次数超过5次即中断程序
        logger.info(cameraPojo.getRtsp() + " 开始推流...");
        // 释放探测时缓存下来的数据帧，避免pts初始值不为0导致画面延时
        grabber.flush();
        int isTest = cameraPojo.getIsTest();
        file = new File(cameraPojo.getRecordDir());

        for (int no_frame_index = 0; no_frame_index < 5 || err_index < 5;) {
            try {
                // 用于中断线程时，结束该循环
                nowThread.sleep(0);
                Frame frame;
                frame = grabber.grab();
                if (null != frame) {
                    // 判断是否需要截图
                    Boolean isSnapshot = ActionController.deviceSnapshotMap.get(Integer.valueOf(cameraPojo.getDeviceId()));
                    if (isSnapshot != null && isSnapshot) {
                        // 如果是正在截图，那么就把帧数据复制一份，并写入到磁盘和数据库
                        final Frame snapshotFrame = frame.clone();
                        ActionController.executor.execute(() -> {
                            takeSnapshot(snapshotFrame);
                        });
                        ActionController.deviceSnapshotMap.put(Integer.valueOf(cameraPojo.getDeviceId()), false);
                    }

                    // 如果超过时间最大值则进行重新记录录像
                    this.restartRecorderWithMaxTime();
                    // 如果超过大小最大值则进行重新记录录像
                    this.restartRecorderWithMaxSize();

                    // 如果是测试推流则直接跳出
                    if (isTest == 1) {
                        break;
                    }
                    record1.record(frame);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                // 销毁构造器
                grabber.stop();
                grabber.close();
                record1.stop();
                record1.close();
                logger.info(cameraPojo.getRtsp() + " 中断推流成功！");
                break;
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                err_index++;
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                err_index++;
            }
        }
        // 程序正常结束销毁构造器
        grabber.stop();
        grabber.close();
        record1.stop();
        record1.close();
        logger.info(cameraPojo.getRtsp() + " 推流结束...");
        return this;
    }

    /**
     * 截图
     * @param frame
     */
    private void takeSnapshot(Frame frame) {
        String thumbnailSize = DeviceManagerController.cameraConfigBo.getSnapShootTumbSize();
        Integer thumbnailWidth = Integer.valueOf(thumbnailSize.split("x")[0]);
        Integer thumbnailHeight = Integer.valueOf(thumbnailSize.split("x")[1]);

        // 1. 截图并生成缩略图，写入文件路径
        String snapshotAddress = RecordNameUtils.snapshotFileAddress(StreamNameUtils.rtspPlay(String.valueOf(cameraPojo.getDeviceId()), "1"));
        String thumbnailAddress = RecordNameUtils.thumbnailFileAddress(StreamNameUtils.rtspPlay(String.valueOf(cameraPojo.getDeviceId()), "1"));
        try {
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage image = converter.convert(frame);
            BufferedImage thumbnailImage = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
            thumbnailImage.getGraphics().drawImage(image, 0, 0, thumbnailWidth, thumbnailHeight, null);

            ImageIO.write(thumbnailImage, "jpg", new File(thumbnailAddress));
            ImageIO.write(image, "jpg", new File(snapshotAddress));
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 2. 保存到数据库
        File snapshotFile = new File(snapshotAddress);
        SnapshotInfo snapshotInfo = new SnapshotInfo();
        snapshotInfo.setDeviceBaseId(Integer.valueOf(cameraPojo.getDeviceId()));
        snapshotInfo.setFilePath(snapshotAddress);
        snapshotInfo.setThumbnailPath(thumbnailAddress);
        snapshotInfo.setCreateTime(LocalDateTime.now());
        snapshotInfo.setType(3);
        snapshotInfo.setAlarmType(0);
        snapshotInfo.setFileSize(snapshotFile.length());

        SnapshotInfoServiceImpl snapshotInfoService = (SnapshotInfoServiceImpl) applicationContext.getBean("snapshotInfoServiceImpl");
        snapshotInfoService.save(snapshotInfo);

        // 3. 保存到临时map中，让调用线程返回结果地址
        JSONObject resultJson = new JSONObject();
        resultJson.put("filePath", snapshotAddress);
        resultJson.put("thumbnailPath", thumbnailAddress);
        ActionController.snapshotAddressMap.put(Integer.valueOf(cameraPojo.getDeviceId()), resultJson);
    }

    /**
     * 设置录像推流器参数
     */
    private void setRecordRecorderOption() {
        String recordSize = DeviceManagerController.cameraConfigBo.getRecordSize();
        Integer recordWidth = Integer.valueOf(recordSize.split("x")[0]);
        Integer recordHeight = Integer.valueOf(recordSize.split("x")[1]);

        record1.setFrameRate(framerate);
//        record1.setVideoCodec(AV_CODEC_ID_H264);
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
