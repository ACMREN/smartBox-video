package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.common.result.GBResult;

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
}
