package com.yangjie.JGB28181.entity.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class DeviceBaseInfoVo {
    private Integer deviceId;
    private String ip;
    private String project;
    private String deviceName;
    private String deviceType;
    private String deviceLink;
    private String linkType;
    private String netType;
    private String address;
    private JSONObject location;
    private String regDate;
    private String specification;
}
