package com.yangjie.JGB28181.media.server.remux;

import com.yangjie.JGB28181.media.callback.OnProcessListener;
import org.springframework.context.ApplicationContext;

public abstract class Observer  extends Thread{

	public OnProcessListener onProcessListener = null;

	public abstract void onMediaStream(byte[] data,int offset,int length,boolean isAudio) throws Exception;

	public abstract void onPts(long pts,boolean isAudio); 

	public abstract void startRemux(Integer isTest, Integer cid, Integer toHls, Integer deviceId, String streamName, ApplicationContext applicationContext);

	public abstract void stopRemux();

	public void setOnProcessListener(OnProcessListener onProcessListener){
		this.onProcessListener = onProcessListener;
	}
}
