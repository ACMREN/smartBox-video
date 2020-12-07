package com.yangjie.JGB28181.media.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.callback.OnProcessListener;
import com.yangjie.JGB28181.media.codec.CommonParser;
import com.yangjie.JGB28181.media.server.remux.Observer;
import com.yangjie.JGB28181.media.server.remux.RtmpPusher;
import com.yangjie.JGB28181.media.server.remux.RtmpRecorder;
import org.springframework.util.CollectionUtils;

public  abstract class Server extends CommonParser implements Observable{

	protected List<Observer> observers = new CopyOnWriteArrayList<>();

	public abstract  void startServer(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc,int port,boolean checkSsrc, String deviceId, Integer deviceBaseId);
	public abstract  void stopServer();
	
	public OnProcessListener onProcessListener = null;

	@Override
	public void subscribe(Observer observer) {
		observers.add(observer);
	}
	
	@Override
	protected void onMediaStreamCallBack(byte[] data,int offset,int length,boolean isAudio) throws Exception{
		if(!CollectionUtils.isEmpty(observers)){
			for (Observer observer : observers) {
				if (observer instanceof RtmpPusher && ((RtmpPusher) observer).pis == null) {
					continue;
				}
				if (observer instanceof RtmpRecorder && ((RtmpRecorder) observer).pis == null) {
					continue;
				}
				observer.onMediaStream(data, offset, length,isAudio);
			}
		}
	}
	
	@Override
	protected void onPtsCallBack(long pts,boolean isAudio){
		if (!CollectionUtils.isEmpty(observers)) {
			for (Observer observer : observers) {
				if(observer != null){
					observer.onPts(pts,isAudio);
				}
			}
		}
	}
	public void setOnProcessListener(OnProcessListener onProcessListener){
		this.onProcessListener = onProcessListener;
	}
}
