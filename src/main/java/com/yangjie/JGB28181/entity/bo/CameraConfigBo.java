package com.yangjie.JGB28181.entity.bo;

import lombok.Data;

@Data
public class CameraConfigBo {
    private String streamMediaIp;
    private String recordDir;
    private String recordStTime;
    private String recordStSize;
    private String recordMaxNum;
    private String recordInterval;
    private String recordMaxRate;
    private String recordSize;
    private String streamMaxRate;
    private String streamSize;
    private String streamInterval;
    private String streamMaxNum;
    private String snapShootDir;
    private String snapShootSize;
    private String snapShootTumbSize;
    private String snapShootQual;
}
