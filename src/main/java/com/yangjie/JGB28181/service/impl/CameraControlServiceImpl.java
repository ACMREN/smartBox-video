package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.HCNetSDK;
import com.yangjie.JGB28181.entity.enumEntity.HikvisionPTZCommandEnum;
import com.yangjie.JGB28181.service.ICameraControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CameraControlServiceImpl implements ICameraControlService {
    Logger logger = LoggerFactory.getLogger(getClass());

    private NativeLong lUserID = new NativeLong(0);

    public static Map<String, NativeLong> deviceLoginStatusMap = new HashMap<>(20);

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
            return this.hikvisionDVRConfig(producer, ip, port, userName, password, HCNetSDK.NET_DVR_GET_PTZPOS, null);
        }

        return GBResult.ok();
    }

    @Override
    public GBResult setDVRConfig(String producer, String ip, Integer port, String userName, String password, Integer command, JSONObject settingJson) {
        if (producer.equals("hikvision")) {
            return this.hikvisionDVRConfig(producer, ip, port, userName, password, HCNetSDK.NET_DVR_SET_PTZPOS, settingJson);
        }

        return GBResult.ok();
    }

    @Override
    public GBResult NET_DVR_PTZSelZoomIn(String producer, String ip, Integer port, String userName, String password, JSONObject framePos) {
        if (producer.equals("hikvision")) {
            return this.hikvisionSetZoomIn(producer, ip, port, userName, password, framePos);
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

        // 1.初始化sdk并登录设备
        GBResult initResult = this.initAndLoginSDK(hcNetSDK, ip, port, userName, password);
        int initCode = initResult.getCode();
        if (initCode != 200) {
            return initResult;
        }

        // 2.控制云台移动
        boolean result = hcNetSDK.NET_DVR_PTZControlWithSpeed_Other(lUserID, new NativeLong(1), PTZCommand, isStop, speed);
        if (!result) {
            logger.info("控制云台移动失败，错误码:" + hcNetSDK.NET_DVR_GetLastError());
            return GBResult.build(500, "控制云台移动失败，错误码:" + hcNetSDK.NET_DVR_GetLastError(), null);
        }

        return GBResult.ok();
    }

    private GBResult hikvisionDVRConfig(String producer, String ip, Integer port, String userName, String password,
                                        int command, JSONObject settingJson) {
        HCNetSDK hcNetSDK = HCNetSDK.INSTANCE;

        // 1.初始化sdk并登录设备
        GBResult initResult = this.initAndLoginSDK(hcNetSDK, ip, port, userName, password);
        int initCode = initResult.getCode();
        if (initCode != 200) {
            return initResult;
        }

        // 2.创建PTZPOS参数对象
        HCNetSDK.NET_DVR_PTZPOS net_dvr_ptzpos = new HCNetSDK.NET_DVR_PTZPOS();
        Pointer pos = net_dvr_ptzpos.getPointer();

        // 如果是获取云台位置
        if (HCNetSDK.NET_DVR_GET_PTZPOS == command) {
            // 3.获取PTZPOS参数
            hcNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_PTZPOS, new NativeLong(1), pos, net_dvr_ptzpos.size(), new IntByReference(0));
            net_dvr_ptzpos.read();

            JSONObject resultJson = new JSONObject();
            resultJson.put("p", net_dvr_ptzpos.wPanPos);
            resultJson.put("t", net_dvr_ptzpos.wTiltPos);
            resultJson.put("z", net_dvr_ptzpos.wZoomPos);
            return GBResult.ok(resultJson);
        }
        // 如果是设置云台位置
        if (HCNetSDK.NET_DVR_SET_PTZPOS == command) {
            System.out.println("=============开始写入位置参数=============");
            // 3.设置PTZPOS参数
            Integer pPos = settingJson.getInteger("pPos");
            Integer tPos = settingJson.getInteger("tPos");
            Integer zPos = settingJson.getInteger("zPos");

            net_dvr_ptzpos.wAction = 1;
            net_dvr_ptzpos.wPanPos = pPos.shortValue();
            net_dvr_ptzpos.wTiltPos = tPos.shortValue();
            net_dvr_ptzpos.wZoomPos = zPos.shortValue();

            net_dvr_ptzpos.write();
            System.out.println("=============结束写入位置参数=============");
            System.out.println("=============位置参数：p:" + pPos + ",t:" + tPos + ", z:" + zPos + "=============");
            Double parseP = HexToDecMa(pPos.shortValue());
            Double parseT = HexToDecMa(tPos.shortValue());
            Double parseZ = HexToDecMa(zPos.shortValue());
            System.out.println("=============转换位置参数：parseP:" + parseP + ",parseT:" + parseT + ", parseZ:" + parseZ + "=============");


            hcNetSDK.NET_DVR_SetDVRConfig(lUserID, command, new NativeLong(1), pos, net_dvr_ptzpos.size());

            return GBResult.ok();
        }

        return GBResult.ok();
    }

    private GBResult hikvisionSetZoomIn(String producer, String ip, Integer port, String userName, String password, JSONObject framePos) {
        HCNetSDK hcNetSDK = HCNetSDK.INSTANCE;

        // 1.初始化sdk并登录设备
        GBResult initResult = this.initAndLoginSDK(hcNetSDK, ip, port, userName, password);
        int initCode = initResult.getCode();
        if (initCode != 200) {
            return initResult;
        }

        HCNetSDK.NET_DVR_POINT_FRAME net_dvr_point_frame = new HCNetSDK.NET_DVR_POINT_FRAME();
        net_dvr_point_frame.xTop = framePos.getInteger("left");
        net_dvr_point_frame.xBottom = framePos.getInteger("right");
        net_dvr_point_frame.yTop = framePos.getInteger("top");
        net_dvr_point_frame.yBottom = framePos.getInteger("bottom");
        net_dvr_point_frame.write();

        hcNetSDK.NET_DVR_PTZSelZoomIn_EX(lUserID, new NativeLong(1), net_dvr_point_frame);

        return GBResult.ok();
    }

    private GBResult initAndLoginSDK(HCNetSDK hcNetSDK, String ip, Integer port, String userName, String password) {
        // 1.初始化sdk
        boolean initSuc = hcNetSDK.NET_DVR_Init();
        if (!initSuc) {
            logger.info("初始化sdk失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
            return GBResult.build(500, "初始化设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError(), null);
        }
        // 2.登录设备
        String key = ip + ":" + port;
        NativeLong lUserId = deviceLoginStatusMap.get(key);
        if (null == lUserId || lUserId.intValue() <= 0) {
            lUserId = hcNetSDK.NET_DVR_Login_V30(ip, port.shortValue(), userName, password, null);//登陆
            if (lUserId.intValue() < 0) {
                logger.info("登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError());
                return GBResult.build(500, "登录设备失败，错误码：" + hcNetSDK.NET_DVR_GetLastError(), null);
            }
        }

        return GBResult.ok();
    }

    public static Double HexToDecMa(short pos) {
        return Double.valueOf((pos / 4096) * 1000 + ((pos % 4096) / 256) * 100 + ((pos % 256) / 16) * 10 + (pos % 16));
    }

    public static Integer DecToHexMa(Double pos) {
        return (int) Math.floor(4096*Math.floor(pos/1000)+ 256*Math.floor((pos%1000)/100)+16*Math.floor((pos%100)/10)+pos%10);
    }
}
