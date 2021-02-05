package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 子设备信息表
 * </p>
 *
 * @author karl
 * @since 2021-02-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GbClientInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 下级平台平台ip
     */
    private String ip;

    /**
     * 下级平台sip编号
     */
    private String deviceSerialNum;

    /**
     * 下级平台sip域
     */
    private String domain;

    /**
     * 项目名称
     */
    private String project;

    /**
     * 地址名称
     */
    private String address;

    /**
     * 经度
     */
    private String longitude;

    /**
     * 纬度
     */
    private String latitude;

    /**
     * 监控数量
     */
    private Integer cameraNum;

    /**
     * 注册类型：0-链接，1-国标，2-平台
     */
    private Integer linkType;

    /**
     * 网络类型：0-局域网，1-互联网
     */
    private Integer netType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;


}
