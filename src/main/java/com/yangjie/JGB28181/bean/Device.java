package com.yangjie.JGB28181.bean;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Device {

	/**
	 * 设备Id
	 */
	private String deviceId;

	/**
	 * 设备名
	 */
	private String name;

	/**
	 * 传输协议
	 * UDP/TCP
	 */
	private String protocol;

	/**
	 * wan地址
	 */
	private Host host;

	/**
	 * 设备的类型（暂时为platform和camera)
	 */
	private String deviceType;

	/**
	 * 通道列表
	 */
	private Map<String,DeviceChannel> channelMap;

	/**
	 * 通道catalog信息列表
	 */
	private Map<String, String> channelCatalogMap;


	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public Map<String, DeviceChannel> getChannelMap() {
		return channelMap;
	}

	public void setChannelMap(Map<String, DeviceChannel> channelMap) {
		this.channelMap = channelMap;
	}
}
