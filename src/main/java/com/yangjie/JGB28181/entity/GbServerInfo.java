package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.yangjie.JGB28181.entity.bo.HigherServerInfoBo;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.vo.GbClientInfoVo;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 父设备信息表
 * </p>
 *
 * @author karl
 * @since 2021-01-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GbServerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 项目
     */
    private String project;

    /**
     * 设备编号
     */
    private String deviceSerialNum;

    /**
     * 设备国标域
     */
    private String domain;

    /**
     * ip地址
     */
    private String ip;

    /**
     * 端口
     */
    private Integer port;

    /**
     * 认证密码
     */
    private String password;

    /**
     * 本地认证用户
     */
    private String localSerialNum;

    /**
     * 本地ip
     */
    private String localIp;

    /**
     * 本地端口
     */
    private Integer localPort;

    /**
     * 过期时间
     */
    private Integer expireTime;

    /**
     * 注册间隔
     */
    private Integer registerInterval;

    /**
     * 心跳间隔
     */
    private Integer heartBeatInterval;

    /**
     * 目录分组大小
     */
    private Integer catalogSize;

    /**
     * 字符集
     */
    private String charsetCode;

    /**
     * 信令传输协议
     */
    private String transProtocol;

    /**
     * 推流传输协议
     */
    private String streamProtocol;

    /**
     * 传输方式
     */
    private String type;

    /**
     * 在线状态：0-不在线，1-在线
     */
    private Integer status;

    /**
     * 级联摄像头列表
     */
    private String cameraList;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 最后更新时间
     */
    private String lastUpdateTime;

    /**
     * 监控数量
     */
    private Integer cameraNum;

    /**
     * 注册类型：0-链接，1-国标，2-平台
     */
    private Integer linkType;

    public GbServerInfo(HigherServerInfoBo higherServerInfoBo) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.id = higherServerInfoBo.getPid();
        this.linkType = LinkTypeEnum.getDataByName(higherServerInfoBo.getLinkType()).getCode();
        this.deviceSerialNum = higherServerInfoBo.getDstSIP();
        this.domain = higherServerInfoBo.getDomain();
        this.ip = higherServerInfoBo.getDstIp();
        this.port = higherServerInfoBo.getDstPort();
        this.password = higherServerInfoBo.getPassword();
        this.localSerialNum = DeviceManagerController.serverInfoBo.getId();
        this.localIp = DeviceManagerController.serverInfoBo.getHost();
        this.localPort = Integer.valueOf(DeviceManagerController.serverInfoBo.getPort());
        this.expireTime = higherServerInfoBo.getRegValid();
        this.registerInterval = higherServerInfoBo.getRegPeriod();
        this.heartBeatInterval = higherServerInfoBo.getRegHeart();
        this.catalogSize = 1;
        this.charsetCode = higherServerInfoBo.getCharSet();
        this.transProtocol = higherServerInfoBo.getTransProtocol();
        this.streamProtocol = higherServerInfoBo.getStreamProtocol();
        this.createTime = df.format(LocalDateTime.now());
        this.lastUpdateTime = df.format(LocalDateTime.now());
        this.project = higherServerInfoBo.getProject();
        this.name = higherServerInfoBo.getName();
        this.catalogSize = 1;
        this.cameraNum = higherServerInfoBo.getCameraNum();
        this.status = 0;
        StringBuilder sb = new StringBuilder();
        for (Integer deviceBaseId : higherServerInfoBo.getCameraList()) {
            sb.append(deviceBaseId).append(",");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        this.cameraList = sb.toString();
    }

    public GbServerInfo() {
    }
}
