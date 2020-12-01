package com.yangjie.JGB28181.media.server.remux;

import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.utils.IpUtil;
import com.yangjie.JGB28181.common.utils.TimerUtil;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.bo.Config;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.opencv.videoio.Videoio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.Timer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;


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

    public RtspToRtmpPusher() {
        super();
    }

    public RtspToRtmpPusher(CameraPojo cameraPojo) {
        this.cameraPojo = cameraPojo;
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
            grabber.setOption("gpu", "gpu");
            grabber.setVideoOption("resize", "1280x720");
            grabber.setOption("rtsp_transport", "tcp");// tcp用于解决丢包问题
        }

        avutil.av_log_set_level(avutil.AV_LOG_INFO);
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

        record.setVideoOption("crf", "40");// 画面质量参数，0~51；18~28是一个合理范围
        record.setGopSize(2);
        record.setFrameRate(framerate);
        record.setVideoCodecName("h264_nvenc");
//        record.setVideoCodec(AV_CODEC_ID_H264);
        record.setVideoBitrate(20000);
        record.setOption("gpu", "gpu");
        record.setOption("vf", "scale_npp=1280:720");

        record.setAudioChannels(audioChannels);
        record.setAudioBitrate(audioBitrate);
        record.setSampleRate(sampleRate);
        AVFormatContext fc = null;
        if (cameraPojo.getRtmp().indexOf("rtmp") >= 0 || cameraPojo.getRtmp().indexOf("flv") > 0) {
            // 封装格式flv
            record.setFormat("flv");
            record.setAudioCodecName("aac");
            fc = grabber.getFormatContext();
        }
        try {
            record.start(fc);
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

        String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-y", "-vsync", "0", "-hwaccel", "cuvid", "-c:v", "h264_cuvid", "-i", "rtsp://admin:a12345678@203.88.202.226:554/h264/ch1/main/av_stream", "-f", "flv", "-c:v", "h264_nvenc", "-vf", "scale_npp=1280:720", "rtmp://127.0.0.1:1935/live");
        try {
            pb.inheritIO().start().waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        for (int no_frame_index = 0; no_frame_index < 5 || err_index < 5;) {
//            try {
//                // 用于中断线程时，结束该循环
//                nowThread.sleep(0);
////                Frame frame = grabber.grab();
////                if (null != frame) {
////                    record.record(frame);
////                }
//
//                AVPacket pkt = null;
//                // 获取没有解码的音视频帧
//                pkt = grabber.grabPacket();
//                if (pkt == null || pkt.size() <= 0 || pkt.data() == null) {
//                    // 空包记录次数跳过
//                    no_frame_index++;
//                    err_index++;
//                    continue;
//                }
//                if (isTest == 1) {
//                    break;
//                }
//                // 不需要编码直接把音视频帧推出去
//                err_index += (record.recordPacket(pkt) ? 0 : 1);
//
//                String token = cameraPojo.getToken();
//                Long heartbeats = TimerUtil.heartbeatsMap.get(token);
//                if (null != heartbeats) {
//                    heartbeats++;
//                } else {
//                    heartbeats = 1L;
//                }
//                TimerUtil.heartbeatsMap.put(token, heartbeats);
//
//                av_packet_unref(pkt);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                // 销毁构造器
//                grabber.stop();
//                grabber.close();
//                record.stop();
//                record.close();
//                logger.info(cameraPojo.getRtsp() + " 中断推流成功！");
//                break;
//            } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
//                err_index++;
//            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
//                err_index++;
//            }
//        }
        // 程序正常结束销毁构造器
        grabber.stop();
        grabber.close();
        record.stop();
        record.close();
        logger.info(cameraPojo.getRtsp() + " 推流结束...");
        return this;
    }
}