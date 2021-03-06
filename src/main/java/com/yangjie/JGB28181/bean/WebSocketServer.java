package com.yangjie.JGB28181.bean;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.thread.CameraThread;
import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.service.IARService;
import com.yangjie.JGB28181.web.controller.ActionController;
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
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@ServerEndpoint("/video/{token}")
public class WebSocketServer {
    Logger logger = LoggerFactory.getLogger(getClass());

    private static Map<String, Session> clients = new HashMap<>();

    private static ApplicationContext applicationContext;

    private String token = null;

    public static Map<Integer, CameraPojo> deviceCameraPojoMap = new HashMap<>(20);

    public static Map<Integer, Boolean> deviceKeyFrameMap = new HashMap<>(20);

    public static Map<Integer, Boolean> arStreamMap = new HashMap<>(20);

    public static Map<Integer, Thread> deviceDataThreadMap = new HashMap<>(20);

    public static Map<String, Set<Integer>> tokenStreamSetMap = new HashMap<>(100);

    public static Map<Integer, Set<Session>> deviceSessionMap = new HashMap<>(20);

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
        logger.info("调用onclose方法成功");
        if (clients.containsValue(session)) {
            for (String item : clients.keySet()) {
                Session session1 = clients.remove(item);
                if (session.equals(session1)) {
                    logger.info("session符合，判断是否需要关闭推流");
                    // 判断是否需要关闭推流
                    Set<Integer> streamSet = tokenStreamSetMap.get(item);
                    if (CollectionUtils.isEmpty(streamSet)) {
                        return;
                    }
                    logger.info("开始关闭推流");
                    for (Integer stream : streamSet) {
                        // 去掉要关闭的session
                        Set<Session> sessions = deviceSessionMap.get(stream);
                        sessions.remove(session);
                        // 延迟2分钟关闭推流
                        CameraPojo cameraPojo = deviceCameraPojoMap.get(stream);
                        int count = cameraPojo.getCount();
                        if (count - 1 <= 0) {
                            CacheUtil.callEndMap.put(cameraPojo.getToken(), true);
                            this.removeStreamDelay(cameraPojo, stream);
                        } else {
                            synchronized (cameraPojo) {
                                cameraPojo.setCount(count - 1);
                            }
                        }
                    }

                    return;
                }
            }
        }
    }

    private void removeStreamDelay(CameraPojo cameraPojo, Integer stream) {
        String callId = cameraPojo.getToken();
        CacheUtil.scheduledExecutorService.schedule(() -> {
            CameraPojo realTimeCameraPojo = deviceCameraPojoMap.get(stream);
            Boolean endSymbol = CacheUtil.callEndMap.get(callId);
            logger.info("=============设备id：" + stream + ", 关闭推流过程，正在观看推流人数：" + cameraPojo.getCount() + ", 关闭标志：" + endSymbol);
            // 不管是否需要关闭推流，先把观看推流人数-1
            synchronized (realTimeCameraPojo) {
                realTimeCameraPojo.setCount(cameraPojo.getCount() - 1);
            }
            if (!endSymbol) {
                return;
            }
            logger.info("=======================关闭推流，开始================");
            Boolean isRunning = WebSocketServer.arStreamMap.get(stream);
            if (null != isRunning) {
                isRunning = false;
                WebSocketServer.arStreamMap.put(stream, isRunning);
                logger.info("设备关闭推流，设备id" + stream);
            }
            Thread sendDataThread = WebSocketServer.deviceDataThreadMap.remove(stream);
            if (null != sendDataThread) {
                sendDataThread.interrupt();
            }
            deviceCameraPojoMap.remove(stream);
            logger.info("=======================关闭推流，完成================");
        }, 2, TimeUnit.MINUTES);
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
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
                        Set<Session> sessions = WebSocketServer.deviceSessionMap.get(deviceBaseId);
                        if (null == sessions) {
                            sessions = new HashSet<>();
                        }
                        // 如果已经存在有session在推流，则将其踢出关闭
                        if (sessions.size() >= 1) {
                            for (Session item : sessions) {
                                JSONObject closeJson = new JSONObject();
                                closeJson.put("close", true);
                                item.getBasicRemote().sendText(closeJson.toJSONString());
                                item.close();
                                sessions.remove(item);
                            }
                            sessions = new HashSet<>();
                        }
                        sessions.add(session);
                        streamSet.add(deviceBaseId);
                        WebSocketServer.deviceSessionMap.put(deviceBaseId, sessions);
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
            logger.info("=============设备id：" + deviceBaseId + ", 获取推流过程，正在观看推流人数：" + cameraPojo.getCount() + ", 关闭标志：" + false);
            synchronized (cameraPojo) {
                cameraPojo.setCount(cameraPojo.getCount() + 1);
            }
            deviceCameraPojoMap.put(deviceBaseId, cameraPojo);
            CacheUtil.callEndMap.put(cameraPojo.getToken(), false);
            WebSocketServer.deviceKeyFrameMap.put(deviceBaseId, true);
            JSONObject result = new JSONObject();
            result.put("deviceId", deviceBaseId);
            result.put("source", flvAddress);
            return result;
        }
        return null;
    }

    public void sendMessage(String message, Integer deviceId) throws IOException {
        if (null == deviceId) {
            sendAll(message);
        } else {
            Set<Session> sessions = deviceSessionMap.get(deviceId);
            for (Session item : sessions) {
                if (null != item) {
                    synchronized (item) {
                        if (null != item && item.isOpen()) {
                            item.getBasicRemote().sendText(message);
                        }
                    }
                }
            }
        }
    }

    public void sendMessage(String message, String token) throws IOException {
        if (null == token) {
            sendAll(message);
        } else {
            Session session = clients.get(token);
            if (null != session) {
                synchronized (session) {
                    if (null != session && session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                    }
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
