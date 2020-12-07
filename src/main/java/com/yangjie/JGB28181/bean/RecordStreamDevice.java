package com.yangjie.JGB28181.bean;

import com.yangjie.JGB28181.media.codec.Frame;
import com.yangjie.JGB28181.media.server.Server;
import com.yangjie.JGB28181.media.server.remux.Observer;
import lombok.Data;

import javax.sip.Dialog;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;

@Data
public class RecordStreamDevice {
    /**
     * 设备ID
     */
    private String deviceId;
    /**
     * ssrc
     * invite下发时携带字段
     * 终端摄像头推流会携带上
     */
    private Integer ssrc;
    /**
     * sip callId
     */
    private String callId;

    private Dialog dialog;
    /**
     * rtmp推流名称
     */
    private String streamName;

    /**
     * 网络通道ID
     */
    private String channelId;
    /**
     * 是否正在推流
     */
    private boolean pushing;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 推流时间
     */
    private Date pushStreamDate;

    /**
     * 监听的收流端口
     */
    private int port;

    /**
     * 是否是 tcp传输音视频
     */
    private boolean isTcp;

    /**
     * 接受的视频帧队列
     */
    private ConcurrentLinkedDeque<Frame> frameDeque = new ConcurrentLinkedDeque<Frame>();


    /**
     * 接受流的Server
     */
    private Server server;

    /**
     * 处理封装流的监听者
     */
    private Observer observer;

    /**
     * 拉流地址
     */
    private String pullRtmpAddress;


    public RecordStreamDevice(String deviceId, Integer ssrc, String callId, String streamName, int port,
                            boolean isTcp, Server server,Observer observer,String pullRtmpAddress) {
        this.deviceId = deviceId;
        this.ssrc = ssrc;
        this.callId = callId;
        this.streamName = streamName;
        this.port = port;
        this.isTcp = isTcp;
        this.server = server;
        this.observer = observer;
        this.pullRtmpAddress = pullRtmpAddress;
    }
}
