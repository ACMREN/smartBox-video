package com.yangjie.JGB28181.entity;

import lombok.Data;

@Data
public class LiveCamInfoVo {
    // 临时id
    private Integer cid;
    // 设备id（表主键）
    private Integer deviceId;
    // 设备的ip地址
    private String ip;
    // 设备名称
    private String deviceName;
    // 项目名称
    private String project;
    // 连接状态
    private String linkStatus;
    // 连接类型
    private String linkType;
    // 网络类型
    private String netType;
    // 网络状态
    private String netStatus;
    // 最后一次更新时间
    private String lastUpdateTime;
}
