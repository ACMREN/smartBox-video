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
import org.springframework.util.CollectionUtils;
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

    public static Map<Integer, Thread> deviceThreadMap = new HashMap<>(20);

    public static Map<String, Set<Integer>> tokenStreamSetMap = new HashMap<>(100);

    private static IARService arService;

    @Autowired
    public void setArService (IARService arService) {
        WebSocketServer.arService = arService;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        WebSocketServer.applicationContext = applicationContext;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("token")String token) {
        logger.info("有设备请求加入websocket，用户id：" + token);
        this.token = token;
        clients.put(token, session);
    }

    @OnClose
    public void onClose(Session session) {
        if (clients.containsValue(session)) {
            for (String item : clients.keySet()) {
                Session session1 = clients.remove(item);
                if (session.equals(session1)) {
                    // 判断是否需要关闭推流
                    Set<Integer> streamSet = tokenStreamSetMap.get(item);
                    if (!CollectionUtils.isEmpty(streamSet)) {
                        for (Integer stream : streamSet) {
                            System.out.println(stream);
                            CameraPojo cameraPojo = deviceCameraPojoMap.remove(stream);
                            int count = cameraPojo.getCount();
                            if (count - 1 == 0) {
                                Thread thread = WebSocketServer.deviceThreadMap.remove(stream);
                                if (null != thread) {
                                    thread.interrupt();
                                    logger.info("设备关闭推流，设备id" + stream);
                                }
                            } else {
                                cameraPojo.setCount(count - 1);
                            }
                        }
                    }

                    logger.info("有设备关闭了websocket，token：" + token);
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
                    JSONObject arStream = this.getARStream(deviceBaseId, token);
                    if (null != arStream) {
                        Set<Integer> streamSet = WebSocketServer.tokenStreamSetMap.get(token);
                        if (CollectionUtils.isEmpty(streamSet)) {
                            streamSet = new HashSet<>();
                        }
                        streamSet.add(deviceBaseId);
                        WebSocketServer.tokenStreamSetMap.put(token, streamSet);
                        resultJson.add(arStream);
                    }
                }
            }
            result.put("command", command);
            result.put("data", resultJson);
            this.sendMessage(result.toJSONString(), token);
        }
    }

    private JSONObject getARStream(Integer deviceBaseId, String token) {
        CameraPojo cameraPojo = deviceCameraPojoMap.get(deviceBaseId);
        // 判断是否已经存在对应的推流
        if (null == cameraPojo) {
            GBResult result = arService.playARStream(deviceBaseId, token);
            int code = result.getCode();
            if (code == 200) {
                JSONObject resultJson = (JSONObject) result.getData();
                return resultJson;
            }
        } else {
            // 如果已经存在推流，直接返回
            String flvAddress = cameraPojo.getFlv();
            cameraPojo.setCount(cameraPojo.getCount() + 1);
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
            synchronized (session) {
                if (null != session) {
                    session.getAsyncRemote().sendText(message);
                }
            }
        }
    }

    /**
     * 群发消息
     * @param message 消息内容
     */
    private void sendAll(String message) {
        for (Map.Entry<String, Session> sessionEntry : clients.entrySet()) {
            synchronized (sessionEntry.getValue()) {
                if (null != sessionEntry) {
                    sessionEntry.getValue().getAsyncRemote().sendText(message);
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("错误原因:" + error.getMessage());
        error.printStackTrace();
    }
}
