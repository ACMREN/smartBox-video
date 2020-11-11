package com.yangjie.JGB28181.entity.vo;

import lombok.Data;

@Data
public class LiveCamInfoVo {
    // 临时id
    private Integer cid;
    // 设备id（表主键）
    private Integer deviceId;
    // 基础设备id
    private Integer baseDeviceId;
    // rtsp链接
    private String rtspLink;
    // 设备推流id
    private String pushStreamDeviceId;
    // 国标设备的推流通道id
    private String channelId;
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
    /**
     * 是否在录像
     */
    private Integer recording;
    /**
     * 是否在推流
     */
    private Integer streaming;
    /**
     * 截图文件数量
     */
    private Integer shortcutNum;
    /**
     * 本地文件（录像+截图）总大小
     */
    private Long fileSize;
    /**
     * 是否接入AI
     */
    private Integer AIApplied;
    /**
     * 接入级联平台的数量
     */
    private Integer cascadeNum;
}
