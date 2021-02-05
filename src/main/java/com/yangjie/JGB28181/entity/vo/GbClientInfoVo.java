package com.yangjie.JGB28181.entity.vo;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.entity.GbClientInfo;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.enumEntity.NetTypeEnum;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Data
public class GbClientInfoVo {
    private Integer pId;
    private String deviceSerialNum;
    private String ip;
    private String linkType;
    private String netType;
    private String createTime;
    private String lastUpdateTime;
    private Integer cameraNum;
    private String name;
    private String project;
    private String address;
    private JSONObject location;
    private List<LiveCamInfoVo> cameraList = new ArrayList<>();

    public GbClientInfoVo(GbClientInfo gbClientInfo) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.pId = gbClientInfo.getId();
        this.deviceSerialNum = gbClientInfo.getDeviceSerialNum();
        this.ip = gbClientInfo.getIp();
        this.address = gbClientInfo.getAddress();
        this.linkType = LinkTypeEnum.getDataByCode(gbClientInfo.getLinkType()).getName();
        this.netType = NetTypeEnum.getDataByCode(gbClientInfo.getNetType()).getName();
        this.createTime = df.format(gbClientInfo.getCreateTime());
        this.lastUpdateTime = df.format(gbClientInfo.getLastUpdateTime());
        this.cameraNum = gbClientInfo.getCameraNum();
        this.name = gbClientInfo.getAddress();
        this.project = gbClientInfo.getProject();
        this.address = gbClientInfo.getAddress();
        location = new JSONObject();
        location.put("lng", StringUtils.isEmpty(gbClientInfo.getLongitude())? null : gbClientInfo.getLongitude());
        location.put("lat", StringUtils.isEmpty(gbClientInfo.getLatitude())? null : gbClientInfo.getLatitude());
        this.setUpGbClientCameraList();
    }

    private void setUpGbClientCameraList() {
        List<LiveCamInfoVo> allCameraList = DeviceManagerController.liveCamVoList;
        for (LiveCamInfoVo item : allCameraList) {
            String parentSerialNum = item.getParentSerialNum();
            String ip = item.getIp();
            if (this.deviceSerialNum.equals(parentSerialNum) && this.ip.equals(ip)) {
                this.cameraList.add(item);
            }
        }
    }
}
