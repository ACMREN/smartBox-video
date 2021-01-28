package com.yangjie.JGB28181.bean;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.result.MediaData;
import com.yangjie.JGB28181.common.utils.DeviceUtils;
import com.yangjie.JGB28181.common.utils.StreamUtils;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.IARService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;

@Component
@ServerEndpoint("/video/{deviceBaseId}")
public class WebSocketServer {
    Logger logger = LoggerFactory.getLogger(getClass());

    private static Map<Integer, Session> clients = new HashMap<>();

    private Integer deviceBaseId = 0;

    public static Map<Integer, CameraPojo> deviceCameraPojoMap = new HashMap<>(20);

    public static Map<Integer, Boolean> deviceKeyFrameMap = new HashMap<>(20);

    @Autowired
    IARService arService;

    @OnOpen
    public void onOpen(Session session, @PathParam("deviceBaseId")Integer deviceBaseId) {
        logger.info("有设备请求加入websocket，设备id：" + deviceBaseId);
        this.deviceBaseId = deviceBaseId;
        clients.put(deviceBaseId, session);

        CameraPojo cameraPojo = deviceCameraPojoMap.get(deviceBaseId);
        // 判断是否已经存在对应的推流
        if (null == cameraPojo) {
            GBResult result = arService.playARStream(deviceBaseId);
            int code = result.getCode();
            if (code == 200) {
                List<JSONObject> resultList = (List<JSONObject>) result.getData();
                this.sendMessage(resultList.toString(), null);
            }
        } else {
            // 如果已经存在推流，直接返回
            String flvAddress = cameraPojo.getFlv();
            List<JSONObject> resultList = new ArrayList<>();
            JSONObject result = new JSONObject();
            result.put("deviceId", deviceBaseId);
            result.put("source", flvAddress);
            resultList.add(result);
            this.sendMessage(resultList.toString(), null);
        }
    }

    @OnClose
    public void onClose(Session session) {
        logger.info("有设备断开websocket，设备id：" + deviceBaseId);
        clients.remove(deviceBaseId);
    }

    @OnMessage
    public void onMessage(String message) {
        if (message.contains("command")) {
            JSONObject json = JSONObject.parseObject(message);
            String command = json.getString("command");
            List<Integer> deviceBaseIds = json.getObject("deviceIds", ArrayList.class);
            for (Integer deviceBaseId : deviceBaseIds) {
                if (command.equals("getARFrame")) {
                    deviceKeyFrameMap.put(deviceBaseId, true);
                }
                if (command.equals("closeARFrame")) {
                    deviceKeyFrameMap.put(deviceBaseId, false);
                }
            }
        }
    }

    public void sendMessage(String message, Integer deviceBaseId) {
        if (null == deviceBaseId) {
            sendAll(message);
        } else {
            Session session = clients.get(deviceBaseId);
            session.getAsyncRemote().sendText(message);
        }
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
