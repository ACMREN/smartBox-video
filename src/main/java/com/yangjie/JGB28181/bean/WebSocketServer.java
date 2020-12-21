package com.yangjie.JGB28181.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;

@Component
@ServerEndpoint("/video/{deviceBaseId}")
public class WebSocketServer {
    Logger logger = LoggerFactory.getLogger(getClass());

    private static Map<Integer, Session> clients = new HashMap<>();

    private Integer deviceBaseId = 0;

    @OnOpen
    public void onOpen(Session session, @PathParam("deviceBaseId")Integer deviceBaseId) {
        logger.info("有设备请求加入websocket，设备id：" + deviceBaseId);
        this.deviceBaseId = deviceBaseId;
        clients.put(deviceBaseId, session);
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("有设备断开websocket，设备id：" + deviceBaseId);
        clients.remove(deviceBaseId);
    }

    @OnMessage
    public void onMessage(String message) {
        logger.info("发送消息：" + message);
        this.sendAll(message);
    }

    /**
     * 群发消息
     * @param message 消息内容
     */
    private void sendAll(String message) {
        for (Map.Entry<Integer, Session> sessionEntry : clients.entrySet()) {
            sessionEntry.getValue().getAsyncRemote().sendText(message);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("错误原因:" + error.getMessage());
        error.printStackTrace();
    }
}
