package com.yangjie.JGB28181.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.*;
import com.yangjie.JGB28181.entity.bo.ServerInfoBo;
import com.yangjie.JGB28181.entity.enumEntity.NetStatusEnum;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
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

    @PostMapping(value = "getLiveCamList")
    public GBResult getLiveCamList(SearchLiveCamCondition searchLiveCamCondition) {
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

    @GetMapping("test")
    public GBResult testSearch() {
        return GBResult.ok();
    }

}
