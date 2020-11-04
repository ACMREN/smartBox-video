package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.yangjie.JGB28181.common.utils.DateUtils;
import com.yangjie.JGB28181.entity.enumEntity.DeviceLinkEnum;
import com.yangjie.JGB28181.entity.enumEntity.DeviceTypeEnum;
import com.yangjie.JGB28181.entity.vo.CameraInfoVo;
import com.yangjie.JGB28181.entity.vo.DeviceBaseInfoVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author karl
 * @since 2020-10-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DeviceBaseInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 项目名称
     */
    private String project;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型：1-枪机，2-球机，3-半球机
     */
    private Integer deviceType;

    /**
     * 设备连接方式：0-有线，1-无线
     */
    private Integer deviceLink;

    /**
     * 设备地址
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
     * 注册日期
     */
    @TableField("reg_date")
    private LocalDate regDate;

    /**
     * 品牌型号
     */
    private String specification;

    public DeviceBaseInfo (DeviceBaseInfoVo deviceBaseInfoVo) {
        this.id = deviceBaseInfoVo.getDeviceId();
        this.project = deviceBaseInfoVo.getProject();
        this.address = deviceBaseInfoVo.getAddress();
        this.deviceLink = DeviceLinkEnum.getDataByName(deviceBaseInfoVo.getDeviceLink()).getCode();
        this.deviceType = DeviceTypeEnum.getDataByName(deviceBaseInfoVo.getDeviceType()).getCode();
        this.deviceName = deviceBaseInfoVo.getDeviceName();
        this.latitude = deviceBaseInfoVo.getLocation().get("lat").toString();
        this.longitude = deviceBaseInfoVo.getLocation().get("lng").toString();
        this.regDate = DateUtils.strFormatToLocalDate(deviceBaseInfoVo.getRegDate());
        this.specification = deviceBaseInfoVo.getSpecification();
    }

    public DeviceBaseInfo (CameraInfoVo cameraInfoVo) {
        this.id = cameraInfoVo.getDeviceBaseId();
        this.project = cameraInfoVo.getProject();
        this.address = cameraInfoVo.getAddress();
        this.deviceLink = DeviceLinkEnum.getDataByName(cameraInfoVo.getDeviceLink()).getCode();
        this.deviceType = DeviceTypeEnum.getDataByName(cameraInfoVo.getDeviceType()).getCode();
        this.deviceName = cameraInfoVo.getDeviceName();
        this.latitude = cameraInfoVo.getLocation().get("lat").toString();
        this.longitude = cameraInfoVo.getLocation().get("lng").toString();
        this.regDate = DateUtils.strFormatToLocalDate(cameraInfoVo.getRegDate());
        this.specification = cameraInfoVo.getSpecification();
    }

    public DeviceBaseInfo () {}


}
