package com.yangjie.JGB28181.service;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.DeviceBaseInfo;
import com.yangjie.JGB28181.entity.vo.DeviceBaseInfoVo;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;

import java.util.List;
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

    /**
     * 将数据库中设备基本数据转为VO
     * @param deviceBaseInfos
     * @return
     */
    List<DeviceBaseInfoVo> parseDeviceBaseInfoToVo(List<DeviceBaseInfo> deviceBaseInfos);

    /**
     * 获取摄像头的详细信息
     * @param liveCamInfoVos
     * @return
     */
    List<LiveCamInfoVo> getLiveCamDetailInfo(List<LiveCamInfoVo> liveCamInfoVos);

    /**
     * 包装摄像头设备的详细VO
     * @param liveCamInfoVos
     * @param deviceBaseInfoVos
     * @return
     */
    List<JSONObject> packageLiveCamDetailInfoVo(List<LiveCamInfoVo> liveCamInfoVos, List<DeviceBaseInfoVo> deviceBaseInfoVos);

    /**
     * 判断摄像头是否已经注册到数据库
     * @param id
     * @return
     */
    boolean judgeCameraIsRegistered(Integer id);
}
