package com.yangjie.JGB28181.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.DeviceBaseInfo;
import com.yangjie.JGB28181.entity.PageListVo;
import com.yangjie.JGB28181.entity.TreeInfo;
import com.yangjie.JGB28181.entity.bo.CameraConfigBo;
import com.yangjie.JGB28181.entity.bo.ServerInfoBo;
import com.yangjie.JGB28181.entity.enumEntity.NetStatusEnum;
import com.yangjie.JGB28181.entity.enumEntity.TreeTypeEnum;
import com.yangjie.JGB28181.entity.searchCondition.DeviceBaseCondition;
import com.yangjie.JGB28181.entity.searchCondition.SearchDeviceTreeCondition;
import com.yangjie.JGB28181.entity.searchCondition.SearchLiveCamCondition;
import com.yangjie.JGB28181.entity.vo.CameraInfoVo;
import com.yangjie.JGB28181.entity.vo.DeviceBaseInfoVo;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.entity.vo.TreeInfoVo;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.DeviceBaseInfoService;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.service.TreeInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.*;
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
    private CameraInfoService cameraInfoService;

    @Autowired
    private IDeviceManagerService deviceManagerService;

    @Autowired
    private TreeInfoService treeInfoService;

    public static ServerInfoBo serverInfoBo = new ServerInfoBo();

    public static CameraConfigBo cameraConfigBo = new CameraConfigBo();

    public static List<LiveCamInfoVo> liveCamVoList = new ArrayList<>();

    static {
        File file = null;
//        File file = new File("src/main/resources/config.properties");
        try {
            file = ResourceUtils.getFile("classpath:config.properties");
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

            cameraConfigBo.setRecordDir(properties.getProperty("config.recordDir"));
            cameraConfigBo.setRecordStTime(properties.getProperty("config.recordStTime"));
            cameraConfigBo.setRecordStSize(properties.getProperty("config.recordStSize"));
            cameraConfigBo.setRecordMaxNum(properties.getProperty("config.recordMaxNum"));
            cameraConfigBo.setRecordInterval(properties.getProperty("config.recordInterval"));
            cameraConfigBo.setRecordMaxRate(properties.getProperty("config.recordMaxRate"));
            cameraConfigBo.setRecordSize(properties.getProperty("config.recordSize"));
            cameraConfigBo.setStreamMaxRate(properties.getProperty("config.streamMaxRate"));
            cameraConfigBo.setStreamSize(properties.getProperty("config.streamSize"));
            cameraConfigBo.setStreamInterval(properties.getProperty("config.streamInterval"));
            cameraConfigBo.setStreamMaxNum(properties.getProperty("config.streamMaxNum"));
            cameraConfigBo.setSnapShootDir(properties.getProperty("config.snapShootDir"));
            cameraConfigBo.setSnapShootSize(properties.getProperty("config.snapShootSize"));
            cameraConfigBo.setSnapShootTumbSize(properties.getProperty("config.snapShootTumbSize"));
            cameraConfigBo.setSnapShootQual(properties.getProperty("config.snapShootQual"));

            inputStream.close();
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

        // 写入到配置文件
        this.rewriteServiceConfigProperties(serverInfoBo);

        DeviceManagerController.serverInfoBo = serverInfoBo;

        return GBResult.ok();
    }

    /**
     * 获取流媒体服务器的配置
     * @return
     */
    @GetMapping(value = "getCameraConfig")
    public GBResult getCameraConfig() {
        return GBResult.ok(cameraConfigBo);
    }

    @PostMapping(value = "setCameraConfig")
    public GBResult setCameraConfig(@RequestBody CameraConfigBo cameraConfigBo) {
        this.cameraConfigBo.setRecordDir(cameraConfigBo.getRecordDir());
        this.cameraConfigBo.setRecordStTime(cameraConfigBo.getRecordStTime());
        this.cameraConfigBo.setRecordStSize(cameraConfigBo.getRecordStSize());
        this.cameraConfigBo.setRecordMaxNum(cameraConfigBo.getRecordMaxNum());
        this.cameraConfigBo.setRecordInterval(cameraConfigBo.getRecordInterval());
        this.cameraConfigBo.setRecordMaxRate(cameraConfigBo.getStreamMaxRate());
        this.cameraConfigBo.setRecordSize(cameraConfigBo.getRecordSize());
        this.cameraConfigBo.setStreamMaxRate(cameraConfigBo.getStreamMaxRate());
        this.cameraConfigBo.setStreamSize(cameraConfigBo.getStreamSize());
        this.cameraConfigBo.setStreamInterval(cameraConfigBo.getStreamInterval());
        this.cameraConfigBo.setStreamMaxNum(cameraConfigBo.getStreamMaxNum());
        this.cameraConfigBo.setSnapShootDir(cameraConfigBo.getSnapShootDir());
        this.cameraConfigBo.setSnapShootSize(cameraConfigBo.getSnapShootSize());
        this.cameraConfigBo.setSnapShootTumbSize(cameraConfigBo.getSnapShootTumbSize());
        this.cameraConfigBo.setSnapShootQual(cameraConfigBo.getSnapShootQual());

        // 修改写入配置文件
        this.rewriteCameraConfigProperties(cameraConfigBo);

        return GBResult.ok();
    }

    /**
     * 修改写入配置文件
     * @param cameraConfigBo
     */
    private void rewriteCameraConfigProperties(CameraConfigBo cameraConfigBo) {
        File file = null;
        try {
            file = ResourceUtils.getFile("classpath:config.properties");
            FileInputStream inputStream = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(inputStream);

            properties.setProperty("config.recordDir", cameraConfigBo.getRecordDir());
            properties.setProperty("config.recordStTime", cameraConfigBo.getRecordStTime());
            properties.setProperty("config.recordStSize", cameraConfigBo.getRecordStSize());
            properties.setProperty("config.recordMaxNum", cameraConfigBo.getRecordMaxNum());
            properties.setProperty("config.recordInterval", cameraConfigBo.getRecordInterval());
            properties.setProperty("config.recordMaxRate", cameraConfigBo.getRecordMaxRate());
            properties.setProperty("config.recordSize", cameraConfigBo.getRecordSize());
            properties.setProperty("config.streamMaxRate", cameraConfigBo.getStreamMaxRate());
            properties.setProperty("config.streamSize", cameraConfigBo.getStreamSize());
            properties.setProperty("config.streamInterval", cameraConfigBo.getStreamInterval());
            properties.setProperty("config.streamMaxNum", cameraConfigBo.getStreamMaxNum());
            properties.setProperty("config.snapShootDir", cameraConfigBo.getSnapShootDir());
            properties.setProperty("config.snapShootSize", cameraConfigBo.getSnapShootSize());
            properties.setProperty("config.snapShootTumbSize", cameraConfigBo.getSnapShootTumbSize());
            properties.setProperty("config.snapShootQual", cameraConfigBo.getSnapShootQual());

            FileWriter fileWriter = new FileWriter(file);
            properties.store(fileWriter, null);

            fileWriter.flush();
            fileWriter.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 写入到配置文件
     * @param serverInfoBo
     */
    private void rewriteServiceConfigProperties(ServerInfoBo serverInfoBo) {
        try {
            File file = ResourceUtils.getFile("classpath:config.properties");
            FileInputStream inputStream = new FileInputStream(file);
            Properties properties = new Properties();
            properties.load(inputStream);

            properties.setProperty("config.listenPort", serverInfoBo.getPort());
            properties.setProperty("config.password", serverInfoBo.getPw());
            properties.setProperty("config.sipId", serverInfoBo.getId());
            properties.setProperty("config.sipRealm", serverInfoBo.getDomain());
            properties.setProperty("config.rtspPort", serverInfoBo.getRtspPort());
            properties.setProperty("config.udp.min", serverInfoBo.getUdpPort().getString("min"));
            properties.setProperty("config.udp.max", serverInfoBo.getUdpPort().getString("max"));
            properties.setProperty("config.tcp.min", serverInfoBo.getTcpPort().getString("min"));
            properties.setProperty("config.tcp.max", serverInfoBo.getTcpPort().getString("max"));

            FileWriter fileWriter = new FileWriter(file);
            properties.store(fileWriter, null);

            fileWriter.flush();
            fileWriter.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        PageListVo pageResult = new PageListVo();
        if (null != pageNo && null != pageSize) {
            Integer total = resultList.size();
            Integer skipNum = (pageNo - 1) * pageSize;
            resultList = resultList.stream().skip(skipNum).limit(pageSize).collect(Collectors.toList());

            pageResult = new PageListVo(resultList, pageNo, pageSize, total);
        }

        return GBResult.ok(pageResult);
    }

    /**
     * 获取设备的基本信息列表
     * @param deviceBaseCondition
     * @return
     */
    @PostMapping(value = "getDeviceInfo")
    public GBResult getDeviceInfo(@RequestBody DeviceBaseCondition deviceBaseCondition) {
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
    @PostMapping(value = "getDeviceDetail")
    public GBResult getDeviceDetail(@RequestBody DeviceBaseCondition deviceBaseCondition) {
        List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
        List<DeviceBaseInfo> deviceBaseInfos = deviceBaseInfoService.getBaseMapper().selectBatchIds(deviceIds);
        List<CameraInfo> cameraInfos = cameraInfoService.getBaseMapper().selectList(new QueryWrapper<CameraInfo>().in("device_base_id", deviceIds));
        Map<Integer, String> deviceRtspLinkMap = cameraInfos.stream().collect(Collectors.toMap(CameraInfo::getDeviceBaseId, CameraInfo::getRtspLink));
        List<DeviceBaseInfoVo> deviceBaseInfoVos = deviceManagerService.parseDeviceBaseInfoToVo(deviceBaseInfos);
        liveCamVoList = deviceManagerService.getLiveCamDetailInfo(liveCamVoList);

        List<JSONObject> resultList = deviceManagerService.packageLiveCamDetailInfoVo(liveCamVoList, deviceBaseInfoVos, deviceRtspLinkMap);

        return GBResult.ok(resultList);
    }

    /**
     * 注册摄像头/修改摄像头信息
     * @param cameraInfoVos
     * @return
     */
    @PostMapping(value = "registerCamera")
    public GBResult registerCamera(@RequestBody List<CameraInfoVo> cameraInfoVos) {
        deviceManagerService.registerCameraInfo(cameraInfoVos);

        return GBResult.ok();
    }

    @PostMapping(value = "removeCamera")
    public GBResult removeCamera(@RequestBody DeviceBaseCondition deviceBaseCondition) {
        List<Integer> deviceIds = deviceBaseCondition.getDeviceId();
        deviceBaseInfoService.getBaseMapper().deleteBatchIds(deviceIds);
        cameraInfoService.getBaseMapper().delete(new QueryWrapper<CameraInfo>().in("device_base_id", deviceIds));

        deviceManagerService.removeLiveCamInfoVo(deviceIds);

        return GBResult.ok();
    }

    /**
     * 获取树状图
     * @return
     */
    @PostMapping(value = "getDeviceTree")
    public GBResult getDeviceTree(@RequestBody SearchDeviceTreeCondition searchDeviceTreeCondition) {
        Integer userId = searchDeviceTreeCondition.getUserId();
        String treeType = searchDeviceTreeCondition.getTreeType();
        // 根据userId和treeType去获取树状图信息
        int treeTypeCode = TreeTypeEnum.getDataByName(treeType).getCode();
        TreeInfo data = treeInfoService.getDataByUserAndType(userId, treeTypeCode);

        TreeInfoVo result = new TreeInfoVo();
        if (null != data) {
            result = new TreeInfoVo(data);
        }

        return GBResult.ok(result);
    }

    /**
     * 新增/更新树状图
     * searchDeviceTreeCondition
     * @return
     */
    @PostMapping(value = "setDeviceTree")
    public GBResult setDeviceTree(@RequestBody SearchDeviceTreeCondition searchDeviceTreeCondition) {
        Integer userId = searchDeviceTreeCondition.getUserId();
        String treeType = searchDeviceTreeCondition.getTreeType();
        String treeInfo = searchDeviceTreeCondition.getTreeInfo();
        int treeTypeCode = TreeTypeEnum.getDataByName(treeType).getCode();
        TreeInfo data = treeInfoService.getDataByUserAndType(userId, treeTypeCode);
        if (null == data) {
            data = new TreeInfo();
            data.setUserId(userId);
            data.setTreeInfo(treeInfo);
            data.setTreeType(treeTypeCode);
            treeInfoService.getBaseMapper().insert(data);
        } else {
            data.setTreeInfo(treeInfo);
            treeInfoService.updateById(data);
        }

        return GBResult.ok();
    }

    /**
     * 获取轮询列表信息
     * @return
     */
    @PostMapping(value = "getPollingList")
    public GBResult getPollingList(@RequestBody SearchDeviceTreeCondition searchDeviceTreeCondition) {
        Integer userId = searchDeviceTreeCondition.getUserId();
        String treeType = searchDeviceTreeCondition.getTreeType();
        int treeTypeCode = TreeTypeEnum.getDataByName(treeType).getCode();
        TreeInfo data = treeInfoService.getDataByUserAndType(userId, treeTypeCode);

        TreeInfoVo result = new TreeInfoVo(data);

        return GBResult.ok(result);
    }

    /**
     * 更新轮询列表信息
     * @return
     */
    @PostMapping(value = "setPollingList")
    public GBResult setPollingList(@RequestBody SearchDeviceTreeCondition searchDeviceTreeCondition) {
        Integer userId = searchDeviceTreeCondition.getUserId();
        String pollingList = searchDeviceTreeCondition.getPollingList();
        String treeType = searchDeviceTreeCondition.getTreeType();
        int treeTypeCode = TreeTypeEnum.getDataByName(treeType).getCode();
        TreeInfo data = treeInfoService.getDataByUserAndType(userId, treeTypeCode);
        if (null == data) {
            data = new TreeInfo();
            data.setUserId(userId);
            data.setPollingList(pollingList);
            data.setTreeType(treeTypeCode);
            treeInfoService.getBaseMapper().insert(data);
        } else {
            data.setPollingList(pollingList);
            treeInfoService.updateById(data);
        }

        return GBResult.ok();
    }

    @PostMapping("test")
    public GBResult testSearch() {
        return GBResult.ok(liveCamVoList);
    }


    @PostMapping("discoverDevice")
    public GBResult discoverDevice() throws IOException {
        deviceManagerService.updateDevice();
        return GBResult.ok();
    }
}
