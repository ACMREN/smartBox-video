package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.yangjie.JGB28181.entity.enumEntity.LinkStatusEnum;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.enumEntity.NetTypeEnum;
import com.yangjie.JGB28181.entity.vo.CameraInfoVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author karl
 * @since 2020-10-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class CameraInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * rtsp链接
     */
    private String rtspLink;

    /**
     * 基础设备id
     */
    private Integer deviceBaseId;

    /**
     * ip地址
     */
    private String ip;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 项目名称
     */
    private String project;

    /**
     * 注册状态：0-未注册，1-已注册
     */
    private Integer linkStatus;

    /**
     * 注册类型：0-链接，1-国标，2-平台
     */
    private Integer linkType;

    /**
     * 网络类型：0-局域网，1-互联网
     */
    private Integer netType;

    public CameraInfo(CameraInfoVo cameraInfoVo) {
        this.deviceBaseId = cameraInfoVo.getDeviceId();
        this.rtspLink = cameraInfoVo.getRtspLink();
        this.ip = cameraInfoVo.getIp();
        this.deviceName = cameraInfoVo.getDeviceName();
        this.project = cameraInfoVo.getProject();
        this.linkStatus = LinkStatusEnum.REGISTERED.getCode();
        this.linkType = LinkTypeEnum.getDataByName(cameraInfoVo.getLinkType()).getCode();
        this.netType = NetTypeEnum.getDataByName(cameraInfoVo.getNetType()).getCode();
    }

    public CameraInfo() {
    }
}
