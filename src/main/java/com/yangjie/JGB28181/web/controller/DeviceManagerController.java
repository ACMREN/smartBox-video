package com.yangjie.JGB28181.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.DeviceBaseInfo;
import com.yangjie.JGB28181.entity.bo.ServerInfoBo;
import com.yangjie.JGB28181.entity.enumEntity.NetStatusEnum;
import com.yangjie.JGB28181.entity.searchCondition.DeviceBaseCondition;
import com.yangjie.JGB28181.entity.searchCondition.SearchLiveCamCondition;
import com.yangjie.JGB28181.entity.vo.CameraInfoVo;
import com.yangjie.JGB28181.entity.vo.DeviceBaseInfoVo;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.service.DeviceBaseInfoService;
import com.yangjie.JGB28181.service.IDeviceManagerService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Autowired
    private DeviceBaseInfoService deviceBaseInfoService;

    @Autowired
    private IDeviceManagerService deviceManagerService;

    public static ServerInfoBo serverInfoBo = new ServerInfoBo();

    public static List<LiveCamInfoVo> liveCamVoList = new ArrayList<>();

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
        // 如果信令服务器的设置为空，则设置配置中的默认配置
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
    public GBResult setGBInfo(@RequestBody ServerInfoBo serverInfoBo) {
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

    /**
     * 按条件搜索实时摄像头列表
     * @param searchLiveCamCondition
     * @return
     */
    @PostMapping(value = "getLiveCamList")
    public GBResult getLiveCamList(@RequestBody SearchLiveCamCondition searchLiveCamCondition) {
        List<LiveCamInfoVo> resultList = liveCamVoList;
        // 1. 按照搜索条件进行过滤
        if (!StringUtils.isEmpty(searchLiveCamCondition.getLinkType())) {
            resultList = resultList.stream().filter(item -> item.getLinkType().equals(searchLiveCamCondition.getLinkType())).collect(Collectors.toList());
        }
        if (!StringUtils.isEmpty(searchLiveCamCondition.getNetType())) {
            resultList = resultList.stream().filter(item -> item.getNetType().equals(searchLiveCamCondition.getNetType())).collect(Collectors.toList());;
        }
        if (!StringUtils.isEmpty(searchLiveCamCondition.getLinkStatus())) {
            resultList = resultList.stream().filter(item -> item.getLinkStatus().equals(searchLiveCamCondition.getLinkStatus())).collect(Collectors.toList());
        }
        if (!StringUtils.isEmpty(searchLiveCamCondition.getNetStatus())) {
            resultList = resultList.stream().filter(item -> item.getNetStatus().equals(searchLiveCamCondition.getNetStatus())).collect(Collectors.toList());
        }
        if (!StringUtils.isEmpty(searchLiveCamCondition.getKeyword())) {
            String searchKeyword = searchLiveCamCondition.getKeyword();
            resultList = resultList.stream().filter(item -> item.getNetStatus().equals(NetStatusEnum.ONLINE.getName())).collect(Collectors.toList());
            List<LiveCamInfoVo> deviceNameFilterList = resultList.stream().filter(item -> item.getDeviceName().contains(searchKeyword)).collect(Collectors.toList());
            List<LiveCamInfoVo> projectNameFilterList = resultList.stream().filter(item -> item.getProject().contains(searchKeyword)).collect(Collectors.toList());
            // 过滤的数据取并集即过滤的结果
            deviceNameFilterList.removeAll(projectNameFilterList);
            deviceNameFilterList.addAll(projectNameFilterList);
            resultList = deviceNameFilterList;
        }

        // 2. 按照条件进行排序
        String sortKey = searchLiveCamCondition.getSortKey();
        String sortOrder = searchLiveCamCondition.getSortOrder();
        if ("deviceName".equals(sortKey)) {
            resultList = resultList.stream()
                    .sorted("desc".equals(sortOrder)? Comparator.comparing(LiveCamInfoVo::getDeviceName) : Comparator.comparing(LiveCamInfoVo::getDeviceName))
                    .collect(Collectors.toList());
        } else if ("project".equals(sortKey)) {
            resultList = resultList.stream()
                    .sorted("desc".equals(sortOrder)? Comparator.comparing(LiveCamInfoVo::getProject) : Comparator.comparing(LiveCamInfoVo::getProject))
                    .collect(Collectors.toList());
        } else if ("linkStatus".equals(sortKey)) {
            resultList = resultList.stream()
                    .sorted("desc".equals(sortOrder)? Comparator.comparing(LiveCamInfoVo::getLinkStatus) : Comparator.comparing(LiveCamInfoVo::getLinkStatus))
                    .collect(Collectors.toList());
        } else if ("linkType".equals(sortKey)) {
            resultList = resultList.stream()
                    .sorted("desc".equals(sortOrder)? Comparator.comparing(LiveCamInfoVo::getLinkType) : Comparator.comparing(LiveCamInfoVo::getLinkType))
                    .collect(Collectors.toList());
        } else if ("netType".equals(sortKey)) {
            resultList = resultList.stream()
                    .sorted("desc".equals(sortOrder)? Comparator.comparing(LiveCamInfoVo::getNetType) : Comparator.comparing(LiveCamInfoVo::getNetType))
                    .collect(Collectors.toList());
        } else if ("netStatus".equals(sortKey)) {
            resultList = resultList.stream()
                    .sorted("desc".equals(sortOrder)? Comparator.comparing(LiveCamInfoVo::getNetStatus) : Comparator.comparing(LiveCamInfoVo::getNetStatus))
                    .collect(Collectors.toList());
        } else if ("lastUpdateTime".equals(sortKey)) {
            resultList = resultList.stream()
                    .sorted("desc".equals(sortOrder)? Comparator.comparing(LiveCamInfoVo::getLastUpdateTime) : Comparator.comparing(LiveCamInfoVo::getLastUpdateTime))
                    .collect(Collectors.toList());
        }

        // 3. 对最终结果进行分页
        Integer pageNo = searchLiveCamCondition.getPageNo();
        Integer pageSize = searchLiveCamCondition.getPageSize();
        if (null != pageNo && null != pageSize) {
            Integer skipNum = (pageNo - 1) * pageSize;
            resultList = resultList.stream().skip(skipNum).limit(pageSize).collect(Collectors.toList());
        }

        return GBResult.ok(resultList);
    }

    /**
     * 获取设备的基本信息列表
     * @param deviceBaseCondition
     * @return
     */
    @GetMapping(value = "getDeviceInfo")
    public GBResult getDeviceInfo(@ModelAttribute DeviceBaseCondition deviceBaseCondition) {
        List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
        List<DeviceBaseInfo> deviceBaseInfos = deviceBaseInfoService.getBaseMapper().selectBatchIds(deviceIds);
        List<DeviceBaseInfoVo> deviceBaseInfoVos = deviceManagerService.parseDeviceBaseInfoToVo(deviceBaseInfos);

        return GBResult.ok(deviceBaseInfoVos);
    }

    /**
     * 保存设备的基本信息列表
     * @param deviceBaseInfoVo
     * @return
     */
    @PostMapping(value = "setDeviceInfo")
    public GBResult setDeviceInfo(@RequestBody DeviceBaseInfoVo deviceBaseInfoVo) {
        Integer id = deviceBaseInfoVo.getDeviceId();
        DeviceBaseInfo data = deviceBaseInfoService.getBaseMapper().selectById(id);
        DeviceBaseInfo deviceBaseInfo = new DeviceBaseInfo(deviceBaseInfoVo);
        if (null != data) {
            deviceBaseInfoService.getBaseMapper().updateById(deviceBaseInfo);
        } else {
            deviceBaseInfoService.getBaseMapper().insert(deviceBaseInfo);
        }

        return GBResult.ok();
    }

    /**
     * 获取摄像头设备的详细信息
     * @param deviceBaseCondition
     * @return
     */
    @GetMapping(value = "getDeviceDetail")
    public GBResult getDeviceDetail(@ModelAttribute DeviceBaseCondition deviceBaseCondition) {
        List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
        List<DeviceBaseInfo> deviceBaseInfos = deviceBaseInfoService.getBaseMapper().selectBatchIds(deviceIds);
        List<DeviceBaseInfoVo> deviceBaseInfoVos = deviceManagerService.parseDeviceBaseInfoToVo(deviceBaseInfos);
        liveCamVoList = deviceManagerService.getLiveCamDetailInfo(liveCamVoList);

        List<JSONObject> resultList = deviceManagerService.packageLiveCamDetailInfoVo(liveCamVoList, deviceBaseInfoVos);

        return GBResult.ok(resultList);
    }

    /**
     * 注册摄像头/修改摄像头信息
     * @param cameraInfoVos
     * @return
     */
    @PostMapping(value = "registerCamera")
    public GBResult registerCamera(@RequestBody List<CameraInfoVo> cameraInfoVos) {
        List<Integer> cameraDeviceIds = cameraInfoVos.stream().map(CameraInfoVo::getDeviceId).collect(Collectors.toList());
        List<DeviceBaseInfo> deviceBaseInfos = deviceBaseInfoService.getBaseMapper().selectBatchIds(cameraDeviceIds);
        Map<Integer, DeviceBaseInfo> deviceBaseInfoIdMap = deviceBaseInfos.stream().collect(Collectors.toMap(DeviceBaseInfo::getId, Function.identity()));

        for (CameraInfoVo item : cameraInfoVos) {
            Integer cameraDeviceId = item.getDeviceId();
            DeviceBaseInfo deviceBaseInfo = new DeviceBaseInfo(item);
            if (deviceBaseInfoIdMap.containsKey(cameraDeviceId)) {
                deviceBaseInfoService.getBaseMapper().updateById(deviceBaseInfo);
            } else {
                deviceBaseInfoService.getBaseMapper().insert(deviceBaseInfo);
            }
        }

        return GBResult.ok();
    }

    @GetMapping(value = "removeDevice")
    public GBResult removeDevice(@ModelAttribute DeviceBaseCondition deviceBaseCondition) {
        List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
        deviceBaseInfoService.getBaseMapper().deleteBatchIds(deviceIds);

        return GBResult.ok();
    }

    @PostMapping("test")
    public GBResult testSearch(@RequestBody SearchLiveCamCondition condition) {
        System.out.println(condition);
        return GBResult.ok();
    }

}
