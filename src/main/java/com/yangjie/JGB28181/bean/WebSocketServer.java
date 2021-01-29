package com.yangjie.JGB28181.bean;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.service.IARService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.*;

@Component
@ServerEndpoint("/video/{token}")
public class WebSocketServer {
    Logger logger = LoggerFactory.getLogger(getClass());

    private static Map<String, Session> clients = new HashMap<>();

    private static ApplicationContext applicationContext;

    private String token = null;

    public static Map<Integer, CameraPojo> deviceCameraPojoMap = new HashMap<>(20);

    public static Map<Integer, Boolean> deviceKeyFrameMap = new HashMap<>(20);

    @Autowired
    IARService arService;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        WebSocketServer.applicationContext = applicationContext;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token")String token) {
        logger.info("有设备请求加入websocket，用户id：" + token);
        this.token = token;
        clients.put(token, session);
        this.sendMessage("test", null);
    }

    @OnClose
    public void onClose(Session session) {
        if (clients.containsValue(session)) {
            for (String item : clients.keySet()) {
                Session session1 = clients.get(item);
                if (session.equals(session1)) {
                    logger.info("有设备关闭了websocket，token：" + token);
                    clients.remove(token);
                    break;
                }
            }
        }
    }

    @OnMessage
    public void onMessage(String message) {
        if (message.contains("command")) {
            JSONObject json = JSONObject.parseObject(message);
            String token = json.getString("token");
            String command = json.getString("command");
            List<Integer> deviceBaseIds = json.getObject("deviceIds", ArrayList.class);
            List<JSONObject> resultJson = new ArrayList<>();
            JSONObject result = new JSONObject();
            for (Integer deviceBaseId : deviceBaseIds) {
                if (command.equals("getARFrame")) {
                    deviceKeyFrameMap.put(deviceBaseId, true);
                }
                if (command.equals("closeARFrame")) {
                    deviceKeyFrameMap.put(deviceBaseId, false);
                }
                if (command.equals("getVideoStream")) {
                    JSONObject arStream = this.getARStream(deviceBaseId);
                    if (null != arStream) {
                        resultJson.add(arStream);
                    }
                }
            }
            result.put("command", command);
            result.put("data", resultJson);
            this.sendMessage(GBResult.ok(result).toString(), token);
        }
    }

    private JSONObject getARStream(Integer deviceBaseId) {
        arService = (IARService) applicationContext.getBean("arService");
        CameraPojo cameraPojo = deviceCameraPojoMap.get(deviceBaseId);
        // 判断是否已经存在对应的推流
        if (null == cameraPojo) {
            GBResult result = arService.playARStream(deviceBaseId);
            int code = result.getCode();
            if (code == 200) {
                JSONObject resultJson = (JSONObject) result.getData();
                return resultJson;
            }
        } else {
            // 如果已经存在推流，直接返回
            String flvAddress = cameraPojo.getFlv();
            JSONObject result = new JSONObject();
            result.put("deviceId", deviceBaseId);
            result.put("source", flvAddress);
            return result;
        }
        return null;
    }

    public void sendMessage(String message, String token) {
        if (StringUtils.isEmpty(token)) {
            sendAll(message);
        } else {
            Session session = clients.get(token);
            session.getAsyncRemote().sendText(message);
        }
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
