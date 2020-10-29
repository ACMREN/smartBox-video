package com.yangjie.JGB28181.common.scheduleJob;

import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.constants.DeviceConstants;
import com.yangjie.JGB28181.common.utils.DateUtils;
import com.yangjie.JGB28181.entity.*;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UpdateDeviceJob {
    @Autowired
    private IDeviceManagerService deviceManagerService;

    public void updateDevice() {
        // 1. 根据onvif协议搜索内网摄像头
        Set<String> deviceSet = deviceManagerService.discoverDevice();

        // 2. 在redis中查询国标设备
        Set<Device> GBDeviceSet = deviceManagerService.getAllGBDevice();

        // 3. 整理步骤1，2中的数据，去重
        Set<String> onvifDuplicateSet = new HashSet<>();
        Set<String> onvifNoDuplicateSet = new HashSet<>();
        this.removeDuplicateDevice(deviceSet, GBDeviceSet, onvifDuplicateSet, onvifNoDuplicateSet);

        // 4. 把去重后的onvif设备和国标设备放入到数据结构中
        List<LiveCamInfoVo> dataList = packageVoList(onvifDuplicateSet, onvifNoDuplicateSet, GBDeviceSet);
        System.out.println(dataList);

    }

    /**
     * 去掉与国标重复的onvif设备
     * @param onvifDeviceUrlSet
     * @param GBDeviceSet
     * @return
     */
    private void removeDuplicateDevice(Set<String> onvifDeviceUrlSet, Set<Device> GBDeviceSet,
                                       Set<String> onvifDuplicateSet, Set<String> onvifNoDuplicateSet) {
        for (String onvifDeviceUrl : onvifDeviceUrlSet) {
            for (Device GBDevice : GBDeviceSet) {
                String wanIp = GBDevice.getHost().getWanIp();
                if (!onvifDeviceUrl.contains(wanIp)) {
                    onvifNoDuplicateSet.add(onvifDeviceUrl);
                } else {
                    onvifDuplicateSet.add(onvifDeviceUrl);
                }
            }
        }
    }

    private List<LiveCamInfoVo> packageVoList(Set<String> onvifDuplicateSet, Set<String> onvifNoDuplicateSet,
                                              Set<Device> GBDeviceSet) {
        List<LiveCamInfoVo> dataList = new ArrayList<>();
        String updateTime = DateUtils.getFormatDateTime(new Date());
        Random random = new Random();
        // 把国标设备放入到结果list中
        for (Device GBDevice : GBDeviceSet) {
            String wanIp = GBDevice.getHost().getWanIp();
            String deviceType = GBDevice.getDeviceType();
            if (DeviceConstants.DEVICE_TYPE_PLATFORM.equals(deviceType)) {
                continue;
            }
            LiveCamInfoVo data = new LiveCamInfoVo();
            int cid = random.nextInt(10000);
            data.setCid(cid);
            data.setDeviceId(cid);
            data.setIp(wanIp);
            data.setDeviceName("");
            data.setProject("");
            data.setLinkStatus(LinkStatusEnum.UNREGISTERED.getName());
            data.setLinkType(LinkTypeEnum.GB28181.getName());
            data.setNetStatus(NetStatusEnum.ONLINE.getName());
            data.setLastUpdateTime(updateTime);
            // 判断设备的网络类型
            boolean isWan = false;
            for (String onvifDuplicateUrl : onvifDuplicateSet) {
                if (onvifDuplicateUrl.contains(wanIp)) {
                    // 如果是onvif能搜索到，则是局域网类型
                    isWan = true;
                    break;
                }
            }
            if (isWan) {
                data.setNetType(NetTypeEnum.WAN.getName());
            } else {
                // onvif搜索不到，则是互联网类型
                data.setNetType(NetTypeEnum.IT.getName());
            }

            dataList.add(data);
        }

        // 把onvif设备放入到结果list中
        for (String onvifNoDuplicateUrl : onvifNoDuplicateSet) {
            Pattern pattern = Pattern.compile(BaseConstants.IPV4_REGEX);
            Matcher matcher = pattern.matcher(onvifNoDuplicateUrl);
            String ip = "";
            if (matcher.find()) {
                ip = matcher.group();
            }
            LiveCamInfoVo data = new LiveCamInfoVo();
            int cid = random.nextInt(10000);
            data.setCid(cid);
            data.setDeviceId(cid);
            data.setIp(ip);
            data.setDeviceName("");
            data.setProject("");
            data.setLinkStatus(LinkStatusEnum.UNREGISTERED.getName());
            data.setLinkType(LinkTypeEnum.ONVIF.getName());
            data.setNetStatus(NetStatusEnum.ONLINE.getName());
            data.setLastUpdateTime(updateTime);
            data.setNetType(NetTypeEnum.WAN.getName());

            dataList.add(data);
        }
        return dataList;
    }
}
