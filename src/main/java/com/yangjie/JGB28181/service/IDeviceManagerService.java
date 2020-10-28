package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.result.GBResult;

import java.util.Set;

public interface IDeviceManagerService {

    GBResult getLiveCamList();

    /**
     * 根据onvif协议搜索设备
     * @return
     */
    Set<String> discoverDevice();

    /**
     * 获取所有的有记录的国标摄像头
     * @return
     */
    Set<Device> getAllGBDevice();
}
