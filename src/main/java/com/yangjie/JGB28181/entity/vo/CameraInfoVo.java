package com.yangjie.JGB28181.entity.vo;

import lombok.Data;

@Data
public class CameraInfoVo extends DeviceBaseInfoVo {
    /**
     * 作为标识id
     */
    private Integer cid;
    private Integer deviceBaseId;
    private String rtspLink;
    private String deviceSerialNum;
    private String parentSerialNum;

    public CameraInfoVo() {
    }
}
