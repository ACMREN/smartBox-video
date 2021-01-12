package com.yangjie.JGB28181.entity.condition;

import lombok.Data;

@Data
public class GBDevicePlayCondition {
    private Integer id;
    private String deviceId;
    private String channelId;
    private String protocol;
    private Integer isTest;
    private Integer cid;
    private Integer toHls;
    private Integer isRecord;
    private Integer isSwitch;
    private Integer toFlv;
    private Integer toPushStream;
    private Integer toHigherServer;
    private String higherServerIp;
    private Integer higherServerPort;
    private String higherCallId;

    public GBDevicePlayCondition(Integer id, String deviceId, String channelId, String protocol, Integer isTest,
                                 Integer cid, Integer toHls, Integer isRecord, Integer isSwitch, Integer toFlv,
                                 Integer toPushStream, Integer toHigherServer, String higherServerIp, Integer higherServerPort,
                                 String higherCallId) {
        this.id = id;
        this.deviceId = deviceId;
        this.channelId = channelId;
        this.protocol = protocol;
        this.isTest = isTest;
        this.cid = cid;
        this.toHls = toHls;
        this.isRecord = isRecord;
        this.isSwitch = isSwitch;
        this.toFlv = toFlv;
        this.toPushStream = toPushStream;
        this.toHigherServer = toHigherServer;
        this.higherServerIp = higherServerIp;
        this.higherServerPort = higherServerPort;
        this.higherCallId = higherCallId;
    }

    public GBDevicePlayCondition() {};
}
