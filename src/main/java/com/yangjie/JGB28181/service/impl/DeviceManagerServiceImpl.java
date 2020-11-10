package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.constants.DeviceConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.DateUtils;
import com.yangjie.JGB28181.common.utils.RedisUtil;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.DeviceBaseInfo;
import com.yangjie.JGB28181.entity.enumEntity.*;
import com.yangjie.JGB28181.entity.vo.CameraInfoVo;
import com.yangjie.JGB28181.entity.vo.DeviceBaseInfoVo;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.DeviceBaseInfoService;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.apache.cxf.ws.discovery.WSDiscoveryClient;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class DeviceManagerServiceImpl implements IDeviceManagerService {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DeviceBaseInfoService deviceBaseInfoService;

    @Autowired
    CameraInfoService cameraInfoService;

    private static Map<String, Integer> ipDeviceIdMap = new HashMap<>(20);

    @Override
    public GBResult getLiveCamList() {
        return null;
    }

    @Override
    public Set<String> discoverDevice() {
        Set<String> deviceUrlSet = new HashSet<>();
        WSDiscoveryClient client = new WSDiscoveryClient();
        client.setVersion10();
        client.setDefaultProbeTimeout(5000);
        ProbeType probeType = new ProbeType();
        probeType.getTypes().add(new QName("tds:Device"));
        probeType.getTypes().add(new QName("dn:Network Video Transmitter"));
        try {
            ProbeMatchesType probeMatchesType = client.probe(probeType);
            List<ProbeMatchType> probeMatchTypeList = probeMatchesType.getProbeMatch();
            for (ProbeMatchType type : probeMatchTypeList) {
                List<String> xAddrs = type.getXAddrs();
                for (String XAddr : xAddrs) {
                    if (XAddr.matches(BaseConstants.ONVIF_IPV4_REGEX)) {
                        deviceUrlSet.add(XAddr);
                    }
                }
            }
            return deviceUrlSet;
        } catch (Exception e) {
            return deviceUrlSet;
        }
    }

    @Override
    public Set<Device> getAllGBDevice() {
        Map<String, String> deviceStrMap = RedisUtil.scanAllKeys();
        if (null != deviceStrMap) {
            Set<Device> deviceSet = new HashSet<>();
            for (String value : deviceStrMap.values()) {
                Device device = JSONObject.parseObject(value, Device.class);
                deviceSet.add(device);
            }
            return deviceSet;
        }
        return new HashSet<>();
    }

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public void registerCameraInfo(List<CameraInfoVo> cameraInfoVos) {
        List<Integer> cameraDeviceIds = cameraInfoVos.stream().map(CameraInfoVo::getDeviceId).collect(Collectors.toList());
        List<DeviceBaseInfo> deviceBaseInfos = deviceBaseInfoService.getBaseMapper().selectBatchIds(cameraDeviceIds);
        Map<Integer, DeviceBaseInfo> deviceBaseInfoIdMap = deviceBaseInfos.stream().collect(Collectors.toMap(DeviceBaseInfo::getId, Function.identity()));

        List<LiveCamInfoVo> newDataList = new ArrayList<>();
        List<CameraInfo> newCameraInfoList = new ArrayList<>();
        for (CameraInfoVo item : cameraInfoVos) {
            // 先更新基础设备表
            Integer deviceBaseId = item.getDeviceId();
            DeviceBaseInfo deviceBaseInfo = new DeviceBaseInfo(item);
            if (deviceBaseInfoIdMap.containsKey(deviceBaseId)) {
                deviceBaseInfo.setId(deviceBaseId);
                deviceBaseInfoService.getBaseMapper().updateById(deviceBaseInfo);
            } else {
                deviceBaseInfoService.getBaseMapper().insert(deviceBaseInfo);
                item.setDeviceId(deviceBaseInfo.getId());
            }

            // 再更新摄像头设备表
            CameraInfo cameraInfo = cameraInfoService.getDataByDeviceBaseId(deviceBaseId);
            if (null != cameraInfo) {
                Integer cameraId = cameraInfo.getId();
                cameraInfo = new CameraInfo(item);
                cameraInfo.setId(cameraId);
                cameraInfoService.updateById(cameraInfo);
            } else {
                cameraInfo = new CameraInfo(item);
                cameraInfoService.getBaseMapper().insert(cameraInfo);
            }
            newCameraInfoList.add(cameraInfo);
        }

        newDataList = this.parseCameraInfoToLiveCamInfoVo(newCameraInfoList);
        Set<LiveCamInfoVo> liveCamVoSet = new HashSet<>(DeviceManagerController.liveCamVoList);
        liveCamVoSet.addAll(newDataList);
        DeviceManagerController.liveCamVoList = new ArrayList<>(liveCamVoSet);
    }

    @Override
    public List<DeviceBaseInfoVo> parseDeviceBaseInfoToVo(List<DeviceBaseInfo> deviceBaseInfos) {
        List<DeviceBaseInfoVo> resultList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(deviceBaseInfos)) {
            for (DeviceBaseInfo item : deviceBaseInfos) {
                DeviceBaseInfoVo data = new DeviceBaseInfoVo();
                data.setDeviceId(item.getId());
                data.setProject(item.getProject());
                data.setDeviceName(item.getDeviceName());
                data.setAddress(item.getAddress());
                data.setDeviceLink(DeviceLinkEnum.getDataByCode(item.getDeviceLink()).getName());
                data.setDeviceType(DeviceTypeEnum.getDataByCode(item.getDeviceType()).getName());
                JSONObject locationJson = new JSONObject();
                locationJson.put("lng", item.getLongitude());
                locationJson.put("lat", item.getLatitude());
                data.setLocation(locationJson);
                data.setRegDate(DateUtils.localDateFormat(item.getRegDate()));
                data.setSpecification(item.getSpecification());

                resultList.add(data);
            }
        }
        return resultList;
    }

    @Override
    public List<LiveCamInfoVo> getLiveCamDetailInfo(List<LiveCamInfoVo> liveCamInfoVos) {
        if (!CollectionUtils.isEmpty(liveCamInfoVos)) {
            for (LiveCamInfoVo item : liveCamInfoVos) {
                if(ActionController.streamingDeviceMap.containsKey(item.getDeviceId())) {
                    item.setStreaming(1);
                }
                item.setRecording(0);
                item.setShortcutNum(0);
                item.setFileSize(0L);
                item.setAIApplied(0);
                item.setCascadeNum(0);
            }
        }

        return liveCamInfoVos;
    }

    @Override
    public List<JSONObject> packageLiveCamDetailInfoVo(List<LiveCamInfoVo> liveCamInfoVos, List<DeviceBaseInfoVo> deviceBaseInfoVos, Map<Integer, String> deviceRtspLinkMap) {
        List<JSONObject> resultList = new ArrayList<>();
        for (DeviceBaseInfoVo item : deviceBaseInfoVos) {
            Integer camId = item.getDeviceId();
            for (LiveCamInfoVo item1 : liveCamInfoVos) {
                Integer deviceId = item1.getDeviceId();
                if (camId.intValue() == deviceId.intValue()) {
                    String rtspLink = deviceRtspLinkMap.get(deviceId);
                    item1.setRtspLink(rtspLink);
                    JSONObject data = new JSONObject();
                    data.put("deviceBaseInfo", item);
                    data.put("camDetailInfo", item1);

                    resultList.add(data);
                }
            }
        }
        return resultList;
    }

    @Override
    public void updateDevice() throws IOException {
        logger.info("==============================更新设备开始======================");

        // 1. 根据onvif协议搜索内网摄像头
        Set<String> deviceSet = this.discoverDevice();

        // 2. 在redis中查询国标设备
        Set<Device> GBDeviceSet = this.getAllGBDevice();

        // 3. 整理步骤1，2中的数据，去重
        Set<String> onvifDuplicateSet = new HashSet<>();
        Set<String> onvifNoDuplicateSet = new HashSet<>();
        this.removeDuplicateDevice(deviceSet, GBDeviceSet, onvifDuplicateSet, onvifNoDuplicateSet);

        // 4. 把去重后的onvif设备和国标设备放入到数据结构中
        List<LiveCamInfoVo> unregisteredDataList = this.packageVoList(onvifDuplicateSet, onvifNoDuplicateSet, GBDeviceSet);

        // 5. 从数据库中获取已注册的摄像头
        List<CameraInfo> cameraInfoList = cameraInfoService.getAllData();
        List<LiveCamInfoVo> registeredDataList = this.parseCameraInfoToLiveCamInfoVo(cameraInfoList);

        // 6. 数据库中的摄像头数据与步骤四中的数据进行去重
        Set<LiveCamInfoVo> resultSet = this.removeDuplicateLiveCamData(registeredDataList, unregisteredDataList);

        DeviceManagerController.liveCamVoList = new ArrayList<>(resultSet);

        logger.info("==============================更新设备完成======================");
    }

    @Override
    public void removeLiveCamInfoVo(List<Integer> deviceIds) {
        List<LiveCamInfoVo> newLiveCamInfoVos = new ArrayList<>(20);
        List<LiveCamInfoVo> liveCamInfoVos = DeviceManagerController.liveCamVoList;
        for (LiveCamInfoVo item : liveCamInfoVos) {
            Integer baseDeviceId = item.getBaseDeviceId();
            for (Integer deviceId : deviceIds) {
                if (baseDeviceId.intValue() != deviceId) {
                    newLiveCamInfoVos.add(item);
                }
            }
        }

        DeviceManagerController.liveCamVoList = newLiveCamInfoVos;
    }

    /**
     * 去重已注册和未注册的设备，整合最后结果数据
     * @param registeredDataList
     * @param unregisteredDataList
     * @return
     */
    private Set<LiveCamInfoVo> removeDuplicateLiveCamData(List<LiveCamInfoVo> registeredDataList, List<LiveCamInfoVo> unregisteredDataList) throws IOException {
        Set<LiveCamInfoVo> resultList = new HashSet<>();
        // 将未注册的设备放入到结果数据结构中
        for (LiveCamInfoVo item : unregisteredDataList) {
            String unregisteredIp = item.getIp();
            boolean isDuplicate = false;
            for (LiveCamInfoVo item1 : registeredDataList) {
                String registeredIp = item1.getIp();
                if (unregisteredIp.equals(registeredIp)) {
                    // 设置推流设备id
                    item1.setPushStreamDeviceId(item.getPushStreamDeviceId());
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
                if (LinkTypeEnum.RTSP.getName().equals(item.getLinkType())) {
                    String ip = item.getIp();
                    boolean isReachable = Inet4Address.getByName(ip).isReachable(1000);
                    if (isReachable) {
                        item.setNetStatus(NetStatusEnum.ONLINE.getName());
                    } else {
                        item.setNetStatus(NetStatusEnum.OFFLINE.getName());
                    }
                } else {
                    item.setNetStatus(NetStatusEnum.OFFLINE.getName());
                }
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
            data.setDeviceId(item.getDeviceBaseId());
            data.setBaseDeviceId(item.getDeviceBaseId());
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
                                              Set<Device> GBDeviceSet) {
        List<LiveCamInfoVo> dataList = new ArrayList<>();
        String updateTime = DateUtils.getFormatDateTime(new Date());
        Random random = new Random();
        // 把国标设备放入到结果list中
        this.packageGBDeviceToLiveCamVo(GBDeviceSet, dataList, onvifDuplicateSet, random, updateTime);

        // 把onvif设备放入到结果list中
        this.packageOnvifDeviceToLiveCamVo(onvifNoDuplicateSet, dataList, random, updateTime);
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
                                            Random random, String updateTime) {
        for (Device GBDevice : GBDeviceSet) {
            String wanIp = GBDevice.getHost().getWanIp();
            String deviceType = GBDevice.getDeviceType();
            if (DeviceConstants.DEVICE_TYPE_PLATFORM.equals(deviceType)) {
                continue;
            }
            int cid = random.nextInt(10000);
            // 判断此ip是否曾经在此设备连接过
            Integer lastCid = ipDeviceIdMap.get(wanIp);
            if (null != lastCid) {
                cid = lastCid;
            } else {
                ipDeviceIdMap.put(wanIp, cid);
            }
            LiveCamInfoVo data = new LiveCamInfoVo();
            data.setPushStreamDeviceId(GBDevice.getDeviceId());
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
                                               Random random, String updateTime) {
        for (String onvifNoDuplicateUrl : onvifNoDuplicateSet) {
            Pattern pattern = Pattern.compile(BaseConstants.IPV4_REGEX);
            Matcher matcher = pattern.matcher(onvifNoDuplicateUrl);
            String ip = "";
            if (matcher.find()) {
                ip = matcher.group();
            }
            LiveCamInfoVo data = new LiveCamInfoVo();
            int cid = random.nextInt(10000);
            // 判断此ip是否曾经在此设备连接过
            Integer lastCid = ipDeviceIdMap.get(ip);
            if (null != lastCid) {
                cid = lastCid;
            } else {
                ipDeviceIdMap.put(ip, cid);
            }
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

    @Override
    public boolean judgeCameraIsRegistered(Integer id) {
        DeviceBaseInfo deviceBaseInfo = deviceBaseInfoService.getBaseMapper().selectById(id);
        if (null != deviceBaseInfo) {
            return true;
        }
        return false;
    }
}
