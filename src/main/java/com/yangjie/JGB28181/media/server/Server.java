package com.yangjie.JGB28181.media.server;

import java.util.concurrent.ConcurrentLinkedDeque;

import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.callback.OnProcessListener;
import com.yangjie.JGB28181.media.codec.CommonParser;
import com.yangjie.JGB28181.media.server.remux.Observer;

public  abstract class Server extends CommonParser implements Observable{

	protected Observer observer;
	protected Integer toHigherServer;
	protected Integer toPushStream;


	public abstract  void startServer(ConcurrentLinkedDeque<Frame> frameDeque,int ssrc,int port,boolean checkSsrc,
									  String deviceId, Integer deviceBaseId, Integer toPushStream, Integer toHigherServer, String higherServerIp,
									  Integer higherServerPort);
	public abstract  void stopServer();
	
	public OnProcessListener onProcessListener = null;

	@Override
	public void subscribe(Observer observer) {
		System.out.println("注册推流器到服务器");
		this.observer = observer;
	}
	
	@Override
	protected void onMediaStreamCallBack(byte[] data,int offset,int length,boolean isAudio) throws Exception{
		if (null != observer) {
			observer.onMediaStream(data, offset, length,isAudio);
		}
	}
	
	@Override
	protected void onPtsCallBack(long pts,boolean isAudio){
		if (null != observer) {
			observer.onPts(pts,isAudio);
		}
	}

	public void setOnProcessListener(OnProcessListener onProcessListener){
		this.onProcessListener = onProcessListener;
	}

	public void setToHigherServer(Integer toHigherServer) {
		this.toHigherServer = toHigherServer;
	}

	public Integer getToHigherServer() {
		return toHigherServer;
	}

	public Integer getToPushStream() {
		return toPushStream;
	}

	public void setToPushStream(Integer toPushStream) {
		this.toPushStream = toPushStream;
	}
}
