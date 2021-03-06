package com.yangjie.JGB28181.entity.bo;

import lombok.Data;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;

@Data
public class CameraPojo implements Serializable {
	private static final long serialVersionUID = 8183688502930584159L;
	private String username;// 摄像头账号
	private String password;// 摄像头密码
	private String ip;// 摄像头ip
	private String channel;// 摄像头通道
	private String stream;// 摄像头码流
	private String rtsp;// rtsp地址
	private String rtmp;// rtmp地址
	private String hls;// hls地址
	private String url;// 播放地址
	private String hlsUrl;// hls播放地址
	private String flv;	// flv播放地址
	private String openTime;// 打开时间
	private volatile int count = 0;// 使用人数
	private int isTest;//是否测试
	private Integer cid;
	private String token;
	private Integer toHls;
	private Integer toFlv;
	private String deviceId;// 设备的基础id
	private Integer isRecord;// 是否录像功能
	private Integer isSwitch;// 是否开启录像功能：0-关闭，1-开启
	private String recordDir;// 录像路径

	private ApplicationContext applicationContext;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public String getRtsp() {
		return rtsp;
	}

	public void setRtsp(String rtsp) {
		this.rtsp = rtsp;
	}

	public String getRtmp() {
		return rtmp;
	}

	public void setRtmp(String rtmp) {
		this.rtmp = rtmp;
	}

	public String getOpenTime() {
		return openTime;
	}

	public void setOpenTime(String openTime) {
		this.openTime = openTime;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getIsTest() {
		return isTest;
	}

	public void setIsTest(int isTest) {
		this.isTest = isTest;
	}

	public Integer getCid() {
		return cid;
	}

	public void setCid(Integer cid) {
		this.cid = cid;
	}

	public Integer getToHls() {
		return toHls;
	}

	public void setToHls(Integer toHls) {
		this.toHls = toHls;
	}

	public String getHls() {
		return hls;
	}

	public void setHls(String hls) {
		this.hls = hls;
	}

	public String getHlsUrl() {
		return hlsUrl;
	}

	public void setHlsUrl(String hlsUrl) {
		this.hlsUrl = hlsUrl;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	@Override
	public String toString() {
		return "CameraPojo{" +
				"username='" + username + '\'' +
				", password='" + password + '\'' +
				", ip='" + ip + '\'' +
				", channel='" + channel + '\'' +
				", stream='" + stream + '\'' +
				", rtsp='" + rtsp + '\'' +
				", rtmp='" + rtmp + '\'' +
				", hls='" + hls + '\'' +
				", url='" + url + '\'' +
				", hlsUrl='" + hlsUrl + '\'' +
				", flv='" + flv + '\'' +
				", openTime='" + openTime + '\'' +
				", count=" + count +
				", isTest=" + isTest +
				", cid=" + cid +
				", token='" + token + '\'' +
				", toHls=" + toHls +
				", toFlv=" + toFlv +
				", deviceId='" + deviceId + '\'' +
				", isRecord=" + isRecord +
				", isSwitch=" + isSwitch +
				", recordDir='" + recordDir + '\'' +
				", applicationContext=" + applicationContext +
				'}';
	}
}
