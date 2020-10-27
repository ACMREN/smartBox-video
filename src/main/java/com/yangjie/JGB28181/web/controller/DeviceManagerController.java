package com.yangjie.JGB28181.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.ServerInfoBo;
import com.yangjie.JGB28181.entity.TestTable;
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
import java.util.Properties;

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

    @GetMapping("testSearch")
    public GBResult testSearch() {
        TestTable data = testTableService.testGetData();

        return GBResult.ok(data);
    }


}
