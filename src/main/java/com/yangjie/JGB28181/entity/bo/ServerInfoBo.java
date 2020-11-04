package com.yangjie.JGB28181.entity.bo;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import lombok.Data;

import java.util.Map;

@Data
public class ServerInfoBo {
    private String id;

    private String domain;

    private String pw;

    private String host;

    private String port;

    private String rtspPort;

    private JSONObject udpPort;

    private JSONObject tcpPort;

    private Map<String, Device> subDeviceMap;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPw() {
        return pw;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getRtspPort() {
        return rtspPort;
    }

    public void setRtspPort(String rtspPort) {
        this.rtspPort = rtspPort;
    }

    public JSONObject getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(JSONObject udpPort) {
        this.udpPort = udpPort;
    }

    public JSONObject getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(JSONObject tcpPort) {
        this.tcpPort = tcpPort;
    }
}
