package com.yangjie.JGB28181.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.constants.DeviceConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.DateUtils;
import com.yangjie.JGB28181.entity.*;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.service.TestTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("device")
public class DeviceManagerController {
    @Value("${config.listenPort}")
    private String listenPort;

    @Value("${config.sipId}")
    private String sipId;

    @Value("${config.sipRealm}")
    private String sipRealm;

    @Value("${config.password}")
    private String password;

    private String host;

    private String rtspPort;

    private JSONObject udpPort;

    private JSONObject tcpPort;

    public static ServerInfoBo serverInfoBo = new ServerInfoBo();

    @Autowired
    private TestTableService testTableService;

    @Autowired
    private IDeviceManagerService deviceManagerService;

    @Autowired
    private CameraInfoService cameraInfoService;

    static {
        File file = new File("src/main/resources/config.properties");
        try {
            FileInputStream inputStream = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(inputStream);

            InetAddress localHost = Inet4Address.getLocalHost();
            String host = localHost.getHostAddress();
            serverInfoBo.setHost(host);
            serverInfoBo.setPort(properties.getProperty("config.listenPort"));
            serverInfoBo.setDomain(properties.getProperty("config.sipRealm"));
            serverInfoBo.setId(properties.getProperty("config.sipId"));
            serverInfoBo.setPw(properties.getProperty("config.password"));
            serverInfoBo.setRtspPort("10544");

            JSONObject udpPort = new JSONObject();
            udpPort.put("min", 10000);
            udpPort.put("max", 20000);
            serverInfoBo.setUdpPort(udpPort);

            JSONObject tcpPort = new JSONObject();
            tcpPort.put("min", 10000);
            tcpPort.put("max", 20000);
            serverInfoBo.setTcpPort(tcpPort);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取信令服务器的信息
     * @return
     * @throws UnknownHostException
     */
    @GetMapping(value = "getGBInfo")
    public GBResult getGBInfo() throws UnknownHostException {
        if (null == serverInfoBo) {
            serverInfoBo = new ServerInfoBo();
            serverInfoBo.setId(sipId);
            serverInfoBo.setDomain(sipRealm);
            serverInfoBo.setPort(listenPort);
            serverInfoBo.setPw(password);
            serverInfoBo.setRtspPort("10544");
            // 获取服务器的ip地址
            if (StringUtils.isEmpty(host)) {
                InetAddress localHost = Inet4Address.getLocalHost();
                String host = localHost.getHostAddress();
                serverInfoBo.setHost(host);
            } else {
                serverInfoBo.setHost(host);
            }
            if (null == udpPort) {
                udpPort = new JSONObject();
                udpPort.put("min", 10000);
                udpPort.put("max", 20000);
            }
            serverInfoBo.setUdpPort(udpPort);
            if (null == tcpPort) {
                tcpPort = new JSONObject();
                tcpPort.put("min", 10000);
                tcpPort.put("max", 20000);
            }
            serverInfoBo.setTcpPort(tcpPort);
        }

        return GBResult.ok(serverInfoBo);
    }

    /**
     * 设置信令服务器的信息
     * @param serverInfoBo
     * @return
     */
    @PostMapping(value = "setGBInfo")
    public GBResult setGBInfo(ServerInfoBo serverInfoBo) {
        this.host = serverInfoBo.getHost();
        this.listenPort = serverInfoBo.getPort();
        this.password = serverInfoBo.getPw();
        this.sipId = serverInfoBo.getId();
        this.sipRealm = serverInfoBo.getDomain();
        this.rtspPort = serverInfoBo.getRtspPort();
        this.udpPort = serverInfoBo.getUdpPort();
        this.tcpPort = serverInfoBo.getTcpPort();

        DeviceManagerController.serverInfoBo = serverInfoBo;

        return GBResult.ok();
    }

    @PostMapping
    public GBResult getLiveCamList() {

        return GBResult.ok();
    }

    @GetMapping("test")
    public GBResult testSearch() {
        // 1. 根据onvif协议搜索内网摄像头
        Set<String> deviceSet = deviceManagerService.discoverDevice();

        // 2. 在redis中查询国标设备
        Set<Device> GBDeviceSet = deviceManagerService.getAllGBDevice();

        // 3. 整理步骤1，2中的数据，去重
        Set<String> onvifDuplicateSet = new HashSet<>();
        Set<String> onvifNoDuplicateSet = new HashSet<>();
        this.removeDuplicateDevice(deviceSet, GBDeviceSet, onvifDuplicateSet, onvifNoDuplicateSet);

        // 4. 把去重后的onvif设备和国标设备放入到数据结构中
        List<LiveCamInfoVo> unregisteredDataList = packageVoList(onvifDuplicateSet, onvifNoDuplicateSet, GBDeviceSet);

        // 5. 从数据库中获取已注册的摄像头
        List<CameraInfo> cameraInfoList = cameraInfoService.getAllData();
        List<LiveCamInfoVo> registeredDataList = parseCameraInfoToLiveCamInfoVo(cameraInfoList);

        // TODO 6. 数据库中的摄像头数据与步骤四中的数据进行去重
        Set<LiveCamInfoVo> resultSet = removeDuplicateLiveCamData(registeredDataList, unregisteredDataList);


        return GBResult.ok(resultSet);
    }

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
                    item1.setNetStatus(NetStatusEnum.ONLINE.name());
                    isDuplicate = true;
                }
            }
            if (!isDuplicate) {
                resultList.add(item);
            }
        }

        // 将已注册的设备放入到结果数据结构中
        for (LiveCamInfoVo item : registeredDataList) {
            item.setNetStatus(NetStatusEnum.OFFLINE.name());
            resultList.add(item);
        }

        return resultList;
    }

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
