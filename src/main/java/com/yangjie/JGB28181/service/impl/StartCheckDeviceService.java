package com.yangjie.JGB28181.service.impl;

import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.constants.DeviceConstants;
import com.yangjie.JGB28181.common.utils.DateUtils;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.enumEntity.LinkStatusEnum;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.enumEntity.NetStatusEnum;
import com.yangjie.JGB28181.entity.enumEntity.NetTypeEnum;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 服务启动时开始第一次的设备搜索
 */
@Component
public class StartCheckDeviceService implements CommandLineRunner {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    IDeviceManagerService deviceManagerService;

    @Autowired
    CameraInfoService cameraInfoService;


    @Override
    public void run(String... strings) throws Exception {
        logger.info("==============================springboot启动，更新设备开始======================");

        // 1. 根据onvif协议搜索内网摄像头
        Set<String> deviceSet = deviceManagerService.discoverDevice();

        // 2. 在redis中查询国标设备
        Set<Device> GBDeviceSet = deviceManagerService.getAllGBDevice();

        // 3. 整理步骤1，2中的数据，去重
        Set<String> onvifDuplicateSet = new HashSet<>();
        Set<String> onvifNoDuplicateSet = new HashSet<>();
        this.removeDuplicateDevice(deviceSet, GBDeviceSet, onvifDuplicateSet, onvifNoDuplicateSet);

        // 4. 把去重后的onvif设备和国标设备放入到数据结构中
        Map<String, Integer> liveCamIpCidMap = DeviceManagerController.liveCamVoList.stream().collect(Collectors.toMap(LiveCamInfoVo::getIp, LiveCamInfoVo::getCid));
        List<LiveCamInfoVo> unregisteredDataList = packageVoList(onvifDuplicateSet, onvifNoDuplicateSet, GBDeviceSet, liveCamIpCidMap);

        // 5. 从数据库中获取已注册的摄像头
        List<CameraInfo> cameraInfoList = cameraInfoService.getAllData();
        List<LiveCamInfoVo> registeredDataList = parseCameraInfoToLiveCamInfoVo(cameraInfoList);

        // 6. 数据库中的摄像头数据与步骤四中的数据进行去重
        Set<LiveCamInfoVo> resultSet = removeDuplicateLiveCamData(registeredDataList, unregisteredDataList);

        DeviceManagerController.liveCamVoList = new ArrayList<>(resultSet);

        logger.info("==============================springboot启动，更新设备完成======================");
    }

    /**
     * 去重已注册和未注册的设备，整合最后结果数据
     * @param registeredDataList
     * @param unregisteredDataList
     * @return
     */
    private Set<LiveCamInfoVo> removeDuplicateLiveCamData(List<LiveCamInfoVo> registeredDataList, List<LiveCamInfoVo> unregisteredDataList) {
        Set<LiveCamInfoVo> resultList = new HashSet<>();
        // 将未注册的设备放入到结果数据结构中
        for (LiveCamInfoVo item : unregisteredDataList) {
            String unregisteredIp = item.getIp();
            boolean isDuplicate = false;
            for (LiveCamInfoVo item1 : registeredDataList) {
                String registeredIp = item1.getIp();
                if (unregisteredIp.equals(registeredIp)) {
                    // 把重复的已注册的设备设置为在线状态
                    item1.setNetStatus(NetStatusEnum.ONLINE.getName());
                    isDuplicate = true;
                }
            }
            if (!isDuplicate) {
                resultList.add(item);
            }
        }

        // 将已注册的设备放入到结果数据结构中
        for (LiveCamInfoVo item : registeredDataList) {
            if (!NetStatusEnum.ONLINE.getName().equals(item.getNetStatus())) {
                item.setNetStatus(NetStatusEnum.OFFLINE.getName());
            }
            item.setLastUpdateTime(DateUtils.getFormatDateTime(new Date()));
            resultList.add(item);
        }

        return resultList;
    }

    /**
     * 将已注册的设备的BO转为VO
     * @param cameraInfoList
     * @return
     */
    private List<LiveCamInfoVo> parseCameraInfoToLiveCamInfoVo(List<CameraInfo> cameraInfoList) {
        List<LiveCamInfoVo> dataList = new ArrayList<>();
        Random random = new Random();
        for (CameraInfo item : cameraInfoList) {
            LiveCamInfoVo data = new LiveCamInfoVo();
            data.setCid(random.nextInt(10000));
            data.setDeviceId(item.getId());
            data.setIp(item.getIp());
            data.setDeviceName(item.getDeviceName());
            data.setProject(item.getProject());
            data.setLinkStatus(LinkStatusEnum.REGISTERED.getName());
            data.setLinkType(LinkTypeEnum.getDataByCode(item.getLinkType()).getName());
            data.setNetType(NetTypeEnum.getDataByCode(item.getNetType()).getName());

            dataList.add(data);
        }
        return dataList;
    }

    /**
     * 去重onvif设备和国标设备
     * @param onvifDeviceUrlSet
     * @param GBDeviceSet
     * @param onvifDuplicateSet
     * @param onvifNoDuplicateSet
     */
    private void removeDuplicateDevice(Set<String> onvifDeviceUrlSet, Set<Device> GBDeviceSet,
                                       Set<String> onvifDuplicateSet, Set<String> onvifNoDuplicateSet) {
        for (String onvifDeviceUrl : onvifDeviceUrlSet) {
            boolean isWan = false;
            for (Device GBDevice : GBDeviceSet) {
                String wanIp = GBDevice.getHost().getWanIp();
                if (onvifDeviceUrl.contains(wanIp)) {
                    isWan = true;
                    break;
                }
            }
            if (isWan) {
                onvifDuplicateSet.add(onvifDeviceUrl);
            } else {
                onvifNoDuplicateSet.add(onvifDeviceUrl);
            }
        }
    }

    /**
     * 包装onvif设备和国标设备为VO
     * @param onvifDuplicateSet
     * @param onvifNoDuplicateSet
     * @param GBDeviceSet
     * @return
     */
    private List<LiveCamInfoVo> packageVoList(Set<String> onvifDuplicateSet, Set<String> onvifNoDuplicateSet,
                                              Set<Device> GBDeviceSet, Map<String, Integer> liveCamIpCidMap) {
        List<LiveCamInfoVo> dataList = new ArrayList<>();
        String updateTime = DateUtils.getFormatDateTime(new Date());
        Random random = new Random();
        // 把国标设备放入到结果list中
        this.packageGBDeviceToLiveCamVo(GBDeviceSet, dataList, onvifDuplicateSet, liveCamIpCidMap, random, updateTime);

        // 把onvif设备放入到结果list中
        this.packageOnvifDeviceToLiveCamVo(onvifNoDuplicateSet, dataList, liveCamIpCidMap, random, updateTime);
        return dataList;
    }

    /**
     * 包装国标设备为VO
     * @param GBDeviceSet
     * @param dataList
     * @param onvifDuplicateSet
     * @param random
     * @param updateTime
     */
    private void packageGBDeviceToLiveCamVo(Set<Device> GBDeviceSet, List<LiveCamInfoVo> dataList, Set<String> onvifDuplicateSet,
                                            Map<String, Integer> liveCamIpCidMap, Random random, String updateTime) {
        for (Device GBDevice : GBDeviceSet) {
            String wanIp = GBDevice.getHost().getWanIp();
            String deviceType = GBDevice.getDeviceType();
            if (DeviceConstants.DEVICE_TYPE_PLATFORM.equals(deviceType)) {
                continue;
            }
            LiveCamInfoVo data = new LiveCamInfoVo();
            int cid = random.nextInt(10000);
            // 如果数据已经存在，保持cid不变
            if (liveCamIpCidMap.containsKey(wanIp)) {
                data.setCid(liveCamIpCidMap.get(wanIp));
            } else {
                data.setCid(cid);
            }
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
    }

    /**
     * 包装onvif设备为VO
     * @param onvifNoDuplicateSet
     * @param dataList
     * @param random
     * @param updateTime
     */
    private void packageOnvifDeviceToLiveCamVo(Set<String> onvifNoDuplicateSet, List<LiveCamInfoVo> dataList,
                                               Map<String, Integer> liveCamIpCidMap, Random random, String updateTime) {
        for (String onvifNoDuplicateUrl : onvifNoDuplicateSet) {
            Pattern pattern = Pattern.compile(BaseConstants.IPV4_REGEX);
            Matcher matcher = pattern.matcher(onvifNoDuplicateUrl);
            String ip = "";
            if (matcher.find()) {
                ip = matcher.group();
            }
            LiveCamInfoVo data = new LiveCamInfoVo();
            int cid = random.nextInt(10000);
            // 如果数据已经存在，保持cid不变
            if (liveCamIpCidMap.containsKey(ip)) {
                data.setCid(liveCamIpCidMap.get(ip));
            } else {
                data.setCid(cid);
            }
            data.setCid(cid);
            data.setDeviceId(cid);
            data.setIp(ip);
            data.setDeviceName("");
            data.setProject("");
            data.setLinkStatus(LinkStatusEnum.UNREGISTERED.getName());
            data.setLinkType(LinkTypeEnum.RTSP.getName());
            data.setNetStatus(NetStatusEnum.ONLINE.getName());
            data.setLastUpdateTime(updateTime);
            data.setNetType(NetTypeEnum.WAN.getName());

            dataList.add(data);
        }
    }
}
