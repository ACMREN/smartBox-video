package com.yangjie.JGB28181.entity.bo;

import lombok.Data;

@Data
public class HigherServerInfoBo extends ServerInfoBo {
    /**
     * 数据库id
     */
    private Integer databaseId;

    /**
     * 本地设备编号（sip认证用户）
     */
    private String localSerialNum;

    /**
     * 本地ip地址
     */
    private String localIp;

    /**
     * 本地端口地址
     */
    private Integer localPort;

    /**
     * 注册过期时间
     */
    private Integer expireTime;

    /**
     * 注册间隔
     */
    private Integer registerInterval;

    /**
     * 心跳包发送间隔（keepAlive）
     */
    private Integer heartBeatInterval;

    /**
     * 设备目录大小
     */
    private Integer catalogSize;

    /**
     * 字符集
     */
    private String charsetCode;

    /**
     * 传输协议
     */
    private String protocol;
}
