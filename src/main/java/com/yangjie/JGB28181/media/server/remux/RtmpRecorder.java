package com.yangjie.JGB28181.media.server.remux;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.common.utils.RecordNameUtils;
import com.yangjie.JGB28181.common.utils.StreamNameUtils;
import com.yangjie.JGB28181.entity.RecordVideoInfo;
import com.yangjie.JGB28181.entity.SnapshotInfo;
import com.yangjie.JGB28181.media.session.PushStreamDeviceManager;
import com.yangjie.JGB28181.service.RecordVideoInfoService;
import com.yangjie.JGB28181.service.SnapshotInfoService;
import com.yangjie.JGB28181.service.impl.RecordVideoInfoServiceImpl;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
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

    private RecordVideoInfo recordVideoInfo;

    private ApplicationContext applicationContext;

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

    public RtmpRecorder() {}

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
            log.info("拉流器开启成功");

            recorder = new CustomFFmpegFrameRecorder(address,1280,720,0);

            // 设置recorder的参数
            this.setRecorderOption();
            recorder.start();
            // 在数据库新建录像文件的信息
            this.saveRecordFileInfo(new RecordVideoInfo());

            Frame frame;

            file = new File(address);
            CacheUtil.deviceRecordingMap.put(deviceBaseId, true);
            while(mRunning){
                frame = grabber.grab();
                if (frame != null) {
                    // 判断是否需要截图
                    Boolean isSnapshot = CacheUtil.deviceSnapshotMap.get(deviceBaseId);
                    if (isSnapshot != null && isSnapshot) {
                        // 如果是正在截图，那么就把帧数据复制一份，并写入到磁盘和数据库
                        final Frame snapshotFrame = frame.clone();
                        CacheUtil.executor.execute(() -> {
                            takeSnapshot(snapshotFrame);
                        });
                        CacheUtil.deviceSnapshotMap.put(deviceBaseId, false);
                    }

                    // 如果超过时间最大值则进行重新记录录像
                    this.restartRecorderWithMaxTime();
                    // 如果超过大小最大值则进行重新记录录像
                    this.restartRecorderWithMaxSize();
                    // 如果是测试状态则跳出循环
                    if (isTest == 1) {
                        break;
                    }
                    recorder.record(frame);
                } else if (isTest == 1){
                    CacheUtil.failCidList.add(cid);
                }
            }

        }catch(Exception e){
            e.printStackTrace();
            CacheUtil.deviceRecordingMap.put(deviceBaseId, false);
            PushStreamDeviceManager.mainMap.remove(deviceId);
            log.error("推流发生异常 >>> {} pts== {}" ,e,pts);
            if(onProcessListener != null){
                onProcessListener.onError(callId);
            }
            Thread.currentThread().stop();
        }finally{
            try{
                CacheUtil.deviceRecordingMap.put(deviceBaseId, false);
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
        log.error("录像结束");
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

    private void takeSnapshot(Frame frame) {
        String thumbnailSize = DeviceManagerController.cameraConfigBo.getSnapShootTumbSize();
        Integer thumbnailWidth = Integer.valueOf(thumbnailSize.split("x")[0]);
        Integer thumbnailHeight = Integer.valueOf(thumbnailSize.split("x")[1]);

        // 1. 截图并生成缩略图，写入文件路径
        String snapshotAddress = RecordNameUtils.snapshotFileAddress(StreamNameUtils.rtspPlay(deviceBaseId.toString(), "1"));
        String thumbnailAddress = RecordNameUtils.thumbnailFileAddress(StreamNameUtils.rtspPlay(deviceBaseId.toString(), "1"));
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
        snapshotInfo.setDeviceBaseId(deviceBaseId);
        snapshotInfo.setFilePath(snapshotAddress);
        snapshotInfo.setThumbnailPath(thumbnailAddress);
        snapshotInfo.setCreateTime(LocalDateTime.now());
        snapshotInfo.setType(3);
        snapshotInfo.setAlarmType(0);
        snapshotInfo.setFileSize(snapshotFile.length());

        SnapshotInfoService snapshotInfoService = (SnapshotInfoService) applicationContext.getBean("snapshotInfoService");
        snapshotInfoService.save(snapshotInfo);

        // 3. 保存到临时map中，让调用线程返回结果地址
        JSONObject resultJson = new JSONObject();
        resultJson.put("filePath", snapshotAddress);
        resultJson.put("thumbnailPath", thumbnailAddress);
        CacheUtil.snapshotAddressMap.put(deviceBaseId, resultJson);
    }

    /**
     * 在数据库新建录像文件的信息
     * @param recordVideoInfo
     */
    private void saveRecordFileInfo(RecordVideoInfo recordVideoInfo) {
        this.recordVideoInfo = recordVideoInfo;
        if (null == recordVideoInfo.getDeviceBaseId()) {
            this.recordVideoInfo.setDeviceBaseId(deviceBaseId);
        }
        if (null == recordVideoInfo.getStartTime()) {
            this.recordVideoInfo.setStartTime(LocalDateTime.now());
        }
        if (null == recordVideoInfo.getFilePath()) {
            this.recordVideoInfo.setFilePath(address);
        }
        RecordVideoInfoServiceImpl recordVideoInfoService = (RecordVideoInfoServiceImpl) applicationContext.getBean("recordVideoInfoServiceImpl");
        recordVideoInfoService.saveOrUpdate(this.recordVideoInfo);
    }

    private void addMetadataToRecordVideo(String fileName, String filePath) {
        try {
            String cmd = "cd " + filePath + ";yamdi -i " + fileName + " -o tmp.flv;" + "sleep 0.01;" + "rm -rf " + fileName + ";mv tmp.flv " + fileName;
            String[] cmdArray = new String[]{"/bin/sh", "-c", cmd};

            Process process = Runtime.getRuntime().exec(cmdArray);
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
            recordVideoInfo.setEndTime(LocalDateTime.now());
            recordVideoInfo.setFileSize(file.length());
            // 更新原有的录像文件信息
            this.saveRecordFileInfo(recordVideoInfo);
            // 给录像文件插入元数据
            int index = file.getPath().lastIndexOf("/");
            String filePath = file.getPath().substring(0, index);
            String fileName = file.getName();
            this.addMetadataToRecordVideo(fileName, filePath);

            address = RecordNameUtils.recordVideoFileAddress(streamName);
            file = new File(address);
            recorder = new CustomFFmpegFrameRecorder(address, 1280, 720);
            this.setRecorderOption();
            recorder.start();

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
            recorder.stop();

            recordVideoInfo.setEndTime(LocalDateTime.now());
            recordVideoInfo.setFileSize(file.length());
            // 更新原有的录像文件信息
            this.saveRecordFileInfo(recordVideoInfo);
            // 给录像文件插入元数据
            int index = file.getPath().lastIndexOf("/");
            String filePath = file.getPath().substring(0, index);
            String fileName = file.getName();
            this.addMetadataToRecordVideo(fileName, filePath);

            address = RecordNameUtils.recordVideoFileAddress(streamName);
            file = new File(address);
            recorder = new CustomFFmpegFrameRecorder(address, 1280, 720);
            this.setRecorderOption();
            recorder.start();

            // 新建一条录像文件信息
            this.saveRecordFileInfo(new RecordVideoInfo());
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
            recordVideoInfo.setEndTime(LocalDateTime.now());
            recordVideoInfo.setFileSize(file.length());
            this.saveRecordFileInfo(recordVideoInfo);
        } catch (Exception e) {
            log.info(e.getMessage());
            e.printStackTrace();
        }
        this.mRunning = false;
    }
    @Override
    public void startRemux(Integer isTest, Integer cid, Integer toHls, Integer deviceId, String streamName, ApplicationContext applicationContext) {
        this.cid = cid;
        this.isTest = isTest;
        this.toHls = toHls;
        this.deviceBaseId = deviceId;
        this.streamName = streamName;
        this.applicationContext = applicationContext;
        this.start();
    }
}
