package com.yangjie.JGB28181.service.impl;

import com.sun.jna.NativeLong;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.HCNetSDK;
import com.yangjie.JGB28181.entity.enumEntity.HikvisionPTZCommandEnum;
import com.yangjie.JGB28181.service.ICameraControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CameraControlServiceImpl implements ICameraControlService {
    Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    public GBResult cameraMove(String producer, String ip, Integer port, String userName, String password, String PTZCommand, Integer speed, Integer isStop) {
        if (producer.equals("hikvision")) {
            return this.hikvisionMoveCamera(ip, port, userName, password, HikvisionPTZCommandEnum.getDataByName(PTZCommand).getCode(), speed, isStop);
        } else if (producer.equals("dahua")) {

        }

        return GBResult.ok();
    }

    /**
     * 开航摄像头云台移动
     * @param ip
     * @param port
     * @param userName
     * @param password
     * @param PTZCommand
     * @param speed
     * @param isStop
     * @return
     */
    private GBResult hikvisionMoveCamera(String ip, Integer port, String userName, String password, Integer PTZCommand, Integer speed, Integer isStop) {
        HCNetSDK hcNetSDK = HCNetSDK.INSTANCE;

        HCNetSDK.NET_DVR_CLIENTINFO m_strClientInfo = null;
        NativeLong lUserID;//用户句柄
        NativeLong lPreviewHandle;//预览句柄

        //初始化sdk
        boolean initSuc = hcNetSDK.NET_DVR_Init();
        if (!initSuc) {
            logger.info("初始化sdk失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
            return GBResult.build(500, "初始化设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError(), null);
        }
        // 登录设备
        lUserID = hcNetSDK.NET_DVR_Login_V30(ip, port.shortValue(), userName, password, null);//登陆
        if (lUserID.intValue() < 0) {
            logger.info("登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
            return GBResult.build(500, "登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError(), null);
        }

        m_strClientInfo = new HCNetSDK.NET_DVR_CLIENTINFO();//预览参数 用户参数
        m_strClientInfo.lChannel = new NativeLong(1);

        // 控制云台移动
        boolean result = hcNetSDK.NET_DVR_PTZControlWithSpeed_Other(lUserID, m_strClientInfo.lChannel, PTZCommand, isStop, speed);
        if (!result) {
            logger.info("控制云台移动失败，错误码:" + hcNetSDK.NET_DVR_GetLastError());
            return GBResult.build(500, "控制云台移动失败，错误码:" + hcNetSDK.NET_DVR_GetLastError(), null);
        }

        return GBResult.ok();
    }
}
