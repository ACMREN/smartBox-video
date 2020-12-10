package com.yangjie.JGB28181.service;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.ControlParam;
import com.yangjie.JGB28181.bean.HikvisionControlParam;
import com.yangjie.JGB28181.common.result.GBResult;

import java.util.List;

public interface ICameraControlService {

    /**
     * 操作云台移动
     * @param ip
     * @param port
     * @param userName
     * @param password
     * @param PTZCommand
     * @param speed
     * @param isStop
     * @return
     */
    GBResult cameraMove(String producer, String ip, Integer port, String userName, String password,
                        String PTZCommand, Integer speed, Integer isStop);

    GBResult getDVRConfig(String producer, String ip, Integer port, String userName, String password,
                          Integer command);

    GBResult setDVRConfig(String producer, String ip, Integer port, String userName, String password,
                          Integer command, JSONObject settingJson);
}
