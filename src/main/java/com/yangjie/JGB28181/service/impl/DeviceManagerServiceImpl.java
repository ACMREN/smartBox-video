package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.DateUtils;
import com.yangjie.JGB28181.common.utils.RedisUtil;
import com.yangjie.JGB28181.entity.DeviceBaseInfo;
import com.yangjie.JGB28181.entity.enumEntity.DeviceLinkEnum;
import com.yangjie.JGB28181.entity.enumEntity.DeviceTypeEnum;
import com.yangjie.JGB28181.entity.vo.DeviceBaseInfoVo;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.service.DeviceBaseInfoService;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.apache.cxf.ws.discovery.WSDiscoveryClient;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.xml.namespace.QName;
import java.util.*;

@Component
public class DeviceManagerServiceImpl implements IDeviceManagerService {

    @Autowired
    private DeviceBaseInfoService deviceBaseInfoService;

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
        return null;
    }

    @Override
    public List<DeviceBaseInfoVo> parseDeviceBaseInfoToVo(List<DeviceBaseInfo> deviceBaseInfos) {
        List<DeviceBaseInfoVo> resultList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(deviceBaseInfos)) {
            for (DeviceBaseInfo item : deviceBaseInfos) {
                DeviceBaseInfoVo data = new DeviceBaseInfoVo();
                data.setDeviceId(item.getId());
                data.setRtspLink(item.getRtspLink());
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
    public List<JSONObject> packageLiveCamDetailInfoVo(List<LiveCamInfoVo> liveCamInfoVos, List<DeviceBaseInfoVo> deviceBaseInfoVos) {
        List<JSONObject> resultList = new ArrayList<>();
        for (DeviceBaseInfoVo item : deviceBaseInfoVos) {
            Integer camId = item.getDeviceId();
            for (LiveCamInfoVo item1 : liveCamInfoVos) {
                Integer deviceId = item1.getDeviceId();
                if (camId.intValue() == deviceId.intValue()) {
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
    public boolean judgeCameraIsRegistered(Integer id) {
        DeviceBaseInfo deviceBaseInfo = deviceBaseInfoService.getBaseMapper().selectById(id);
        if (null != deviceBaseInfo) {
            return true;
        }
        return false;
    }
}
