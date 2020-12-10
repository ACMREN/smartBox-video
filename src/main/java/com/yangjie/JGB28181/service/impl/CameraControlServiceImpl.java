package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.yangjie.JGB28181.bean.ControlParam;
import com.yangjie.JGB28181.bean.HikvisionControlParam;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.HCNetSDK;
import com.yangjie.JGB28181.entity.enumEntity.HikvisionPTZCommandEnum;
import com.yangjie.JGB28181.service.ICameraControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CameraControlServiceImpl implements ICameraControlService {
    Logger logger = LoggerFactory.getLogger(getClass());

    private NativeLong lUserID = new NativeLong(0);

    @Override
    public GBResult cameraMove(String producer, String ip, Integer port, String userName, String password,
                               String PTZCommand, Integer speed, Integer isStop) {
        if (producer.equals("hikvision")) {
            return this.hikvisionMoveCamera(ip, port, userName, password, HikvisionPTZCommandEnum.getDataByName(PTZCommand).getCode(), speed, isStop);
        } else if (producer.equals("dahua")) {

        }

        return GBResult.ok();
    }

    @Override
    public GBResult getDVRConfig(String producer, String ip, Integer port, String userName, String password,
                                 Integer command) {
        if (producer.equals("hikvision")) {
            return this.hikvisionDVRConfig(producer, ip, port, userName, password, command);
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

        //初始化sdk
        boolean initSuc = hcNetSDK.NET_DVR_Init();
        if (!initSuc) {
            System.out.println("初始化sdk失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
            logger.info("初始化sdk失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
            return GBResult.build(500, "初始化设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError(), null);
        }
        // 登录设备
        if (lUserID.intValue() <= 0) {
            this.lUserID = hcNetSDK.NET_DVR_Login_V30(ip, port.shortValue(), userName, password, null);//登陆
            if (lUserID.intValue() < 0) {
                System.out.println("登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
                logger.info("登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
                return GBResult.build(500, "登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError(), null);
            }
        }

        m_strClientInfo = new HCNetSDK.NET_DVR_CLIENTINFO();//预览参数 用户参数
        m_strClientInfo.lChannel = new NativeLong(1);

        // 控制云台移动
        System.out.println("lUserID:" + lUserID + ", lChannel:" + m_strClientInfo.lChannel + ", PTZCommand:" + PTZCommand + ", isStop:" + isStop + ", speed:" + speed);
        boolean result = hcNetSDK.NET_DVR_PTZControlWithSpeed_Other(lUserID, m_strClientInfo.lChannel, PTZCommand, isStop, speed);
        if (!result) {
            logger.info("控制云台移动失败，错误码:" + hcNetSDK.NET_DVR_GetLastError());
            return GBResult.build(500, "控制云台移动失败，错误码:" + hcNetSDK.NET_DVR_GetLastError(), null);
        }

        return GBResult.ok();
    }

    private GBResult hikvisionDVRConfig(String producer, String ip, Integer port, String userName, String password,
                                        Integer command) {
        HCNetSDK hcNetSDK = HCNetSDK.INSTANCE;

        HCNetSDK.NET_DVR_CLIENTINFO m_strClientInfo = null;

        //初始化sdk
        boolean initSuc = hcNetSDK.NET_DVR_Init();
        if (!initSuc) {
            logger.info("初始化sdk失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
            return GBResult.build(500, "初始化设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError(), null);
        }
        // 登录设备
        if (lUserID.intValue() <= 0) {
            lUserID = hcNetSDK.NET_DVR_Login_V30(ip, port.shortValue(), userName, password, null);//登陆
            if (lUserID.intValue() < 0) {
                logger.info("登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
                return GBResult.build(500, "登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError(), null);
            }
        }

        m_strClientInfo = new HCNetSDK.NET_DVR_CLIENTINFO();//预览参数 用户参数
        m_strClientInfo.lChannel = new NativeLong(1);

        //创建PTZPOS参数对象
        HCNetSDK.NET_DVR_PTZPOS net_dvr_ptzpos = new HCNetSDK.NET_DVR_PTZPOS();
        Pointer pos = net_dvr_ptzpos.getPointer();
        // 创建PTZSCPOPE参数对象
        HCNetSDK.NET_DVR_PTZSCOPE net_dvr_ptzscope = new HCNetSDK.NET_DVR_PTZSCOPE();
        Pointer scope = net_dvr_ptzscope.getPointer();

        // 获取PTZPOS参数
        hcNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_PTZPOS, new NativeLong(1), pos, net_dvr_ptzpos.size(), new IntByReference(0));
        hcNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_PTZSCOPE, new NativeLong(1), scope, net_dvr_ptzscope.size(), new IntByReference(0));
//        hcNetSDK.NET_DVR_SetDVRConfig(lUserID, command, new NativeLong(1), pos, net_dvr_ptzpos.size());

        net_dvr_ptzpos.read();
        net_dvr_ptzscope.read();

        JSONObject resultJson = new JSONObject();
        resultJson.put("p", net_dvr_ptzpos.wPanPos);
        resultJson.put("pMax", net_dvr_ptzscope.wPanPosMax);
        resultJson.put("pMin", net_dvr_ptzscope.wPanPosMin);
        resultJson.put("t", net_dvr_ptzpos.wTiltPos);
        resultJson.put("tMax", net_dvr_ptzscope.wTiltPosMax);
        resultJson.put("tMin", net_dvr_ptzscope.wTiltPosMin);
        resultJson.put("z", net_dvr_ptzpos.wZoomPos);
        resultJson.put("zMax", net_dvr_ptzscope.wZoomPosMax);
        resultJson.put("zMin", net_dvr_ptzscope.wZoomPosMin);

        return GBResult.ok(resultJson);
    }

    private Double HexToDecMa(short pos) {
        return Double.valueOf((pos / 4096) * 1000 + ((pos % 4096) / 256) * 100 + ((pos % 256) / 16) * 10 + (pos % 16));
    }

    public List<ControlParam> getControlParams(String specification, JSONObject controls) {
        List<ControlParam> controlParamList = new ArrayList<>();
        Integer pSpeed = controls.getInteger("pSpeed");
        Integer tSpeed = controls.getInteger("tSpeed");
        Integer zSpeed = controls.getInteger("zSpeed");
        ControlParam pParam;
        ControlParam tParam;
        ControlParam zParam;
        // 如果是海康设备，则进行海康控制参数的封装
        if (specification.equals("hikvision")) {
            pParam = new HikvisionControlParam();
            tParam = new HikvisionControlParam();
            zParam = new HikvisionControlParam();
            pParam = this.packageHikvisionParam(pSpeed);
            if (pSpeed < 0) {
                ((HikvisionControlParam) pParam).setCommand(HikvisionPTZCommandEnum.RIGHT);
            } else if (pSpeed > 0){
                ((HikvisionControlParam) pParam).setCommand(HikvisionPTZCommandEnum.LEFT);
            }
            tParam = this.packageHikvisionParam(tSpeed);
            if (tSpeed < 0) {
                ((HikvisionControlParam) tParam).setCommand(HikvisionPTZCommandEnum.DOWN);
            } else if (tSpeed > 0){
                ((HikvisionControlParam) tParam).setCommand(HikvisionPTZCommandEnum.UP);
            }
            zParam = this.packageHikvisionParam(zSpeed);
            if (zSpeed < 0) {
                ((HikvisionControlParam) zParam).setCommand(HikvisionPTZCommandEnum.ZOOM_OUT);
            } else if (zSpeed > 0){
                ((HikvisionControlParam) zParam).setCommand(HikvisionPTZCommandEnum.ZOOM_IN);
            }
            controlParamList.add(pParam);
            controlParamList.add(tParam);
            controlParamList.add(zParam);
        }

        return controlParamList;
    }

    private HikvisionControlParam packageHikvisionParam(Integer speed) {
        HikvisionControlParam param = new HikvisionControlParam();
        param.setSpeed(Math.abs(Integer.valueOf(speed)));
        if (speed == 0) {
            param.setIsStop(1);
        } else {
            param.setIsStop(0);
        }
        return param;
    }
}
