package com.yangjie.JGB28181.media.server.remux;

import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.common.utils.RecordNameUtils;
import com.yangjie.JGB28181.common.utils.StreamNameUtils;
import com.yangjie.JGB28181.entity.SnapshotInfo;
import com.yangjie.JGB28181.media.session.PushStreamDeviceManager;
import com.yangjie.JGB28181.service.SnapshotInfoService;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.imageio.ImageIO;

/**
 * rtmp 推流器
 * @author yangjie
 * 2020年3月23日
 */
public class RtmpPusher extends Observer{

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

	private String steamName;

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

	public RtmpPusher(String address,String callId){
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
			avutil.av_log_set_level(avutil.AV_LOG_ERROR);
			grabber.setOption("rtsp_transport", "tcp");
			if (toHls == 1) {
				grabber.setOption("re", "");
			}
			grabber.start();

			recorder = new CustomFFmpegFrameRecorder(address,1280,720,0);

			recorder.setInterleaved(true);
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			recorder.setFormat("flv");
			recorder.setFrameRate(25);
			recorder.setOption("loglevel", "quiet");
			if (toHls == 1) {
				recorder.setOption("hls_time", "10");
			}

			AVFormatContext fc = grabber.getFormatContext();
			recorder.start(fc);
			AVPacket avPacket;
			Frame frame;

			CacheUtil.deviceStreamingMap.put(deviceBaseId, true);
			while(mRunning){
				avPacket=grabber.grabPacket();
				if (avPacket != null && avPacket.size() >0 && avPacket.data() != null) {
					if (isTest == 1) {
						break;
					}
					pts = mPtsQueue.pop();
					recorder.recordPacket(avPacket, pts, pts);
				} else if (isTest == 1){
					CacheUtil.failCidList.add(cid);
				}
			}

		}catch(Exception e){
			CacheUtil.deviceStreamingMap.put(deviceBaseId, false);
			e.printStackTrace();
			PushStreamDeviceManager.mainMap.remove(deviceId);
			log.error("推流发生异常 >>> {} pts== {}" ,e,pts);
			if(onProcessListener != null){
				onProcessListener.onError(callId);
			}
			Thread.currentThread().stop();
		}finally{
			try{
				CacheUtil.deviceStreamingMap.put(deviceBaseId, false);
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


	@Override
	public void stopRemux() {
		try	{
			CacheUtil.deviceStreamingMap.put(deviceBaseId, false);
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
	public void startRemux(Integer isTest, Integer cid, Integer toHls, Integer deviceId, String streamName, ApplicationContext applicationContext) {
		this.cid = cid;
		this.isTest = isTest;
		this.toHls = toHls;
		this.deviceBaseId = deviceId;
		this.steamName = streamName;
		this.applicationContext = applicationContext;
		this.start();
	}

}
