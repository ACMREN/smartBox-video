package com.yangjie.JGB28181.entity.bo;

import com.yangjie.JGB28181.entity.GbServerInfo;
import com.yangjie.JGB28181.entity.enumEntity.NetStatusEnum;
import lombok.Data;

import java.util.List;

@Data
public class HigherServerInfoBo {
    /**
     * 数据库id
     */
    private Integer pid;

    /**
     * 名称
     */
    private String name;

    /**
     * 项目
     */
    private String project;

    /**
     * 目标SIP
     */
    private String dstSIP;

    /**
     * 目标SIP
     */
    private String dstIp;

    /**
     * 目标SIP域
     */
    private String domain;

    /**
     * 认证密码
     */
    private String password;

    /**
     * 目标信令端口
     */
    private Integer dstPort;

    /**
     * 本地设备编号（sip认证用户）
     */
    private String selfSIP;

    /**
     * 本地ip地址
     */
    private String selfIp;

    /**
     * 本地端口地址
     */
    private Integer selfPort;

    /**
     * 注册过期时间
     */
    private Integer regValid;

    /**
     * 注册间隔
     */
    private Integer regPeriod;

    /**
     * 心跳包发送间隔（keepAlive）
     */
    private Integer regHeart;

    /**
     * 设备目录大小
     */
    private Integer catalogSize;

    /**
     * 字符集
     */
    private String charSet;

    /**
     * 信令传输协议
     */
    private String transProtocol;

    /**
     * 推流协议
     */
    private String streamProtocol;

    /**
     * 传输方式
     */
    private String type;

    /**
     * 在线状态
     */
    private String netStatus;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 监控数量
     */
    private Integer cameraNum;

    /**
     * 选取级联的摄像头列表
     */
    private List<Integer> cameraList;

    public HigherServerInfoBo (GbServerInfo gbServerInfo) {
        this.pid = gbServerInfo.getId();
        this.name = gbServerInfo.getName();
        this.project = gbServerInfo.getProject();
        this.selfIp = gbServerInfo.getLocalIp();
        this.selfPort = gbServerInfo.getLocalPort();
        this.selfSIP = gbServerInfo.getLocalSerialNum();
        this.dstIp = gbServerInfo.getIp();
        this.domain = gbServerInfo.getDomain();
        this.dstPort = gbServerInfo.getPort();
        this.dstSIP = gbServerInfo.getDeviceSerialNum();
        this.password = gbServerInfo.getPassword();
        this.regValid = gbServerInfo.getExpireTime();
        this.regPeriod = gbServerInfo.getRegisterInterval();
        this.regHeart = gbServerInfo.getHeartBeatInterval();
        this.charSet = gbServerInfo.getCharsetCode();
        this.type = gbServerInfo.getType();
        this.transProtocol = gbServerInfo.getTransProtocol();
        this.streamProtocol = gbServerInfo.getStreamProtocol();
        this.netStatus = NetStatusEnum.getDataByCode(gbServerInfo.getStatus()).getName();
        this.createTime = gbServerInfo.getCreateTime();
        this.cameraNum = gbServerInfo.getCameraNum();
    }
}
