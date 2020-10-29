package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.RedisUtil;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import org.apache.cxf.ws.discovery.WSDiscoveryClient;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;

@Component
public class DeviceManagerServiceImpl implements IDeviceManagerService {

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
}
