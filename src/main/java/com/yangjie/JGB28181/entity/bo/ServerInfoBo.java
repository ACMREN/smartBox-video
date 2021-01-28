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
}
