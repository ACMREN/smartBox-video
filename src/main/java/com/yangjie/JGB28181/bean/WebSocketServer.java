package com.yangjie.JGB28181.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;

@Component
@ServerEndpoint("/video")
public class WebSocketServer {
    Logger logger = LoggerFactory.getLogger(getClass());

    private static Map<String, Session> clients = new HashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        logger.info("有用户加入websocket");
        clients.put(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("有用户断开websocket");
        clients.remove(session.getId());
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
        for (Map.Entry<String, Session> sessionEntry : clients.entrySet()) {
            sessionEntry.getValue().getAsyncRemote().sendText(message);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("错误原因:" + error.getMessage());
        error.printStackTrace();
    }
}
