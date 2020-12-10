package com.yangjie.JGB28181.media.server.remux;

import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.utils.RecordNameUtils;
import com.yangjie.JGB28181.common.utils.StreamNameUtils;
import com.yangjie.JGB28181.media.session.PushStreamDeviceManager;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RtmpRecorder extends Observer {
    private Logger log = LoggerFactory.getLogger(getClass());

    public PipedInputStream pis;

    public PipedOutputStream pos = new PipedOutputStream();

    private boolean mRunning = true;

    private String address;

    private String callId;

    private long mLastPts;

    private boolean mIs90000TimeBase = false;

    private String deviceId;

    private Integer deviceBaseId;

    private Integer isTest;

    private Integer cid;

    private Integer toHls;

    private String streamName;

    private File file;

    private LocalDate localDate;

    public FFmpegFrameGrabber grabber = null;
    public CustomFFmpegFrameRecorder recorder = null;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    private ConcurrentLinkedDeque<Long> mPtsQueue = new ConcurrentLinkedDeque<>();

    public RtmpRecorder(String address,String callId){
        this.address = address;
        this.callId = callId;
    }
    @Override
    public void onMediaStream(byte[] data, int offset,int length,boolean isAudio) throws Exception{
        if(!isAudio){
            pos.write(data,offset,length);
        }
    }

    /**
     * 有的设备 timebase 90000，有的直接 1000
     */
    @Override
    public void onPts(long pts,boolean isAudio) {
        if(isAudio){
            return;
        }
        //分辨timebase是 90000 还是 1000
        //如果是90000 pts/90
        if(mLastPts == 0 && pts != 0){
            mIs90000TimeBase = (pts >= 3000);
        }
        if(mIs90000TimeBase){
            pts = pts / 90;
        }
        //如果当前pts小于之前的pts
        //推流会崩溃
        //av_write_frame() error -22 while writing video packet.
        if(mLastPts != 0 && pts < mLastPts){
            pts = mLastPts + 40;
        }
        mPtsQueue.add(pts);
        mLastPts = pts;
        //log.info("pts >>> {}",pts);
    }
    @Override
    public void run() {
        Long pts  = 0L;
        try{
            //pis = new PipedInputStream(pos,1024*1024);
            pis = new PipedInputStream(pos, 1024);
            grabber = new FFmpegFrameGrabber(pis,0);
            //阻塞式，直到通道有数据
            grabber.setOption("stimeout", "200000");
            grabber.setOption("y", "");
            grabber.setOption("vsync", "0");
            // 使用硬件加速
            grabber.setOption("hwaccel", "cuvid");
            grabber.setVideoCodecName("h264_cuvid");
            if (toHls == 1) {
                grabber.setOption("re", "");
            }
            avutil.av_log_set_level(avutil.AV_LOG_ERROR);
            grabber.start();

            recorder = new CustomFFmpegFrameRecorder(address,1280,720,0);

            // 设置recorder的参数
            this.setRecorderOption();
            recorder.start();
            AVPacket avPacket;
            Frame frame;

            file = new File(address);
            while(mRunning){
                frame=grabber.grab();
                if (frame != null) {
                    // 如果超过时间最大值则进行重新记录录像
                    this.restartRecorderWithMaxTime();
                    // 如果超过大小最大值则进行重新记录录像
                    this.restartRecorderWithMaxSize();
                    // 如果是测试状态则跳出循环
                    if (isTest == 1) {
                        break;
                    }
                    pts = mPtsQueue.pop();
                    recorder.record(frame);
                } else if (isTest == 1){
                    ActionController.failCidList.add(cid);
                }
            }

        }catch(Exception e){
            e.printStackTrace();
            PushStreamDeviceManager.mainMap.remove(deviceId);
            log.error("推流发生异常 >>> {} pts== {}" ,e,pts);
            if(onProcessListener != null){
                onProcessListener.onError(callId);
            }
            Thread.currentThread().stop();
        }finally{
            try{
                if(recorder != null){
                    recorder.stop();
                    recorder.close();
                }
                if(grabber != null){
                    grabber.stop();
                    grabber.close();
                }
            }catch(Exception e){
                log.info(e.getMessage());
                e.printStackTrace();
            }
        }
        log.error("推流结束");
    }

    /**
     * 设置推流器参数
     */
    private void setRecorderOption() {
        // 获取配置文件中的长跟宽
        String recordSize = DeviceManagerController.cameraConfigBo.getRecordSize();
        Integer width = Integer.valueOf(recordSize.split("x")[0]);
        Integer height = Integer.valueOf(recordSize.split("x")[1]);
        recorder.setImageWidth(width);
        recorder.setImageHeight(height);
        // 推流rtmp的参数
        if (toHls == 0) {
            recorder.setOption("maxrate", DeviceManagerController.cameraConfigBo.getRecordMaxRate());
            recorder.setInterleaved(true);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("flv");
            recorder.setFrameRate(25);
        }

        // 推流hls的参数
        if (toHls == 1) {
            recorder.setOption("maxrate", DeviceManagerController.cameraConfigBo.getRecordMaxRate());
            recorder.setInterleaved(true);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat("flv");
            recorder.setFrameRate(25);
            recorder.setOption("loglevel", "quiet");
        }
    }

    /**
     * 超过单个文件最长时间则进行重新录像
     * @throws FrameRecorder.Exception
     */
    private void restartRecorderWithMaxTime() throws FrameRecorder.Exception {
        long timestamp = recorder.getTimestamp();
        if (timestamp > Long.valueOf(DeviceManagerController.cameraConfigBo.getRecordInterval())) {
            recorder.stop();
            address = RecordNameUtils.recordVideoFileAddress(streamName);
            file = new File(address);
            recorder = new CustomFFmpegFrameRecorder(address, 1280, 720);
            this.setRecorderOption();
            recorder.start();
        }
    }

    /**
     * 超过单个文件最大大小则进行重新录像
     * @throws FrameRecorder.Exception
     */
    private void restartRecorderWithMaxSize() throws FrameRecorder.Exception {
        if (file.length() > Long.valueOf(DeviceManagerController.cameraConfigBo.getRecordStSize())) {
            recorder.stop();
            address = RecordNameUtils.recordVideoFileAddress(streamName);
            file = new File(address);
            recorder = new CustomFFmpegFrameRecorder(address, 1280, 720);
            this.setRecorderOption();
            recorder.start();
        }
    }


    @Override
    public void stopRemux() {
        try	{
            if (null != grabber) {
                grabber.stop();
                grabber.close();
            }
            if (null != recorder) {
                recorder.stop();
                recorder.close();
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            e.printStackTrace();
        }
        this.mRunning = false;
    }
    @Override
    public void startRemux(Integer isTest, Integer cid, Integer toHls, Integer deviceId, String streamName) {
        this.cid = cid;
        this.isTest = isTest;
        this.toHls = toHls;
        this.deviceBaseId = deviceId;
        this.streamName = streamName;
        this.start();
    }
}
