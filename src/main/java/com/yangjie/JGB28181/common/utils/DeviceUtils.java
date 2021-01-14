package com.yangjie.JGB28181.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.bean.DeviceChannel;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.message.SipLayer;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;

import java.util.List;
import java.util.Map;

public class DeviceUtils {

    /**
     * 把rtsp链接转成cameraPojo
     * @param rtspLink
     * @return
     */
    public static CameraPojo parseRtspLinkToCameraPojo(String rtspLink) {
        String paramStr = rtspLink.split("//")[1];
        String username = paramStr.split(":")[0];
        String paramStr1 = paramStr.split(":")[1];
        String password = paramStr1.split("@")[0];
        String ip = paramStr1.split("@")[1];
        String paramStr2 = paramStr.split("@")[1].split(":")[1];
        String channel = paramStr2.split("/")[2].split("ch")[1];
        String stream = paramStr2.split("/")[3];

        CameraPojo cameraPojo = new CameraPojo();
        cameraPojo.setUsername(username);
        cameraPojo.setPassword(password);
        cameraPojo.setIp(ip);
        cameraPojo.setChannel(channel);
        cameraPojo.setStream(stream);

        return cameraPojo;
    }

    /**
     * 根据ip进行国标设备的匹配
     * @param cameraIp
     * @return
     */
    public static JSONObject getLiveCamInfoVoByMatchIp(String cameraIp, String deviceSerialNum, String parentSerialNum) {
        List<LiveCamInfoVo> liveCamInfoVos = DeviceManagerController.liveCamVoList;
        String deviceStr = null;
        String pushStreamDeviceId = null;
        JSONObject dataJson = null;
        for (LiveCamInfoVo item : liveCamInfoVos) {
            String itemIp = item.getIp();
            pushStreamDeviceId = item.getPushStreamDeviceId();
            // 如果ip匹配上，则从redis上获取设备的信息
            if (cameraIp.equals(itemIp) && pushStreamDeviceId.equals(deviceSerialNum)) {
                dataJson = new JSONObject();
                deviceStr = RedisUtil.get(SipLayer.SUB_DEVICE_PREFIX + parentSerialNum);

                Device device = JSONObject.parseObject(deviceStr, Device.class);
                Map<String, DeviceChannel> channelMap = device.getChannelMap();
                if (channelMap.containsKey(deviceSerialNum)) {
                    dataJson.put("deviceStr", deviceStr);
                    dataJson.put("pushStreamDeviceId", pushStreamDeviceId);
                }

            }
        }
        return dataJson;
    }
}
