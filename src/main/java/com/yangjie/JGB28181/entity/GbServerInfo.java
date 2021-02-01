package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
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
}
