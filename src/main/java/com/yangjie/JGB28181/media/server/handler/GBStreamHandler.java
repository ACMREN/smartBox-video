package com.yangjie.JGB28181.media.server.handler;

import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GBStreamHandler<I> extends SimpleChannelInboundHandler<I> {

    protected Bootstrap b = null;

    protected String higherCallId;

    protected String higherServerIp;

    protected Integer higherServerPort;

    protected List<JSONObject> UDPHigherServerInfoList;

    protected Channel UDPChannel = null;

    protected Integer toHigherServer = 0;

    protected Integer toPushStream = 0;

    public Map<String, Channel> callIdChannelMap = new HashMap<>();

    /**
     * 注册到级联平台进行视频流推送
     * @param remoteIp
     * @param remotePort
     */
    public void connectNewRemoteAddress(String remoteIp, Integer remotePort, String higherCallId, boolean isTcp) {
        if (null == this.toHigherServer || this.toHigherServer == 0) {
            this.setToHigherServer(1);
            this.setHigherCallId(higherCallId);
            this.setHigherServerIp(remoteIp);
            this.setHigherServerPort(remotePort);
        } else {
            if (null != b) {
                try {
                    if (isTcp) {
                        ChannelFuture future = b.connect(new InetSocketAddress(remoteIp, remotePort)).sync();
                        Channel channel = future.channel();
                        callIdChannelMap.put(higherCallId, channel);
                    } else {
                        JSONObject higherServerInfo = new JSONObject();
                        higherServerInfo.put("ip", remoteIp);
                        higherServerInfo.put("port", remotePort);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 停止级联平台的视频推送
     * @param higherCallId
     */
    public void disconnectRemoteAddress(String higherCallId) {
        Channel channel = callIdChannelMap.remove(higherCallId);
        channel.disconnect();
    }



    public void setToPushStream(Integer toPushStream) {
        this.toPushStream = toPushStream;
    }

    public Integer getToHigherServer() {
        return toHigherServer;
    }

    public void setToHigherServer(Integer toHigherServer) {
        this.toHigherServer = toHigherServer;
    }

    public void setHigherServerIp(String higherServerIp) {
        this.higherServerIp = higherServerIp;
    }

    public void setHigherServerPort(Integer higherServerPort) {
        this.higherServerPort = higherServerPort;
    }

    public String getHigherCallId() {
        return higherCallId;
    }

    public void setHigherCallId(String higherCallId) {
        this.higherCallId = higherCallId;
    }
}
