package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.bean.PushStreamDevice;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.IDUtils;
import com.yangjie.JGB28181.common.utils.StreamNameUtils;
import com.yangjie.JGB28181.service.IPushStreamService;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class PushHlsStreamServiceImpl implements IPushStreamService {
    public static Map<String, Boolean> deviceInfoMap = new HashMap<>();

    public static Map<String, JSONObject> hlsInfoMap = new HashMap<>();

    public static Map<String, Process> hlsProcessMap = new HashMap<>();

    @Override
    public GBResult pushStream(String deviceId, String channelId) {
        // 1. 开启清理过期的TS索引文件的定时器
        this.cleanUpTempTsFile(deviceId, channelId);

        // 2. 开始推流hls
        String callId = this.pushRtmpToHls(deviceId, channelId);

        return GBResult.build(200, "success", callId);
    }

    public static String pushRtmpToHls(String deviceId, String channelId) {
        // 1. 生成rtmpurl并将rtmp转码成hls进行推流
        String callId = IDUtils.id();
        String playFileName = StreamNameUtils.play(deviceId, channelId);
        String rtmpUrl = BaseConstants.rtmpBaseUrl + playFileName;
        String all = "ffmpeg -re -i " + rtmpUrl + " -loglevel quiet -vcodec libx264 -vprofile baseline -acodec aac -ar 44100 -strict -2 -ac 1 -f flv -s 1280x720 -q 10 -hls_time 10 -hls_wrap 5 rtmp://127.0.0.1:1935/hls/" + playFileName;
        // 2. 把HLS信息放到静态map中
        JSONObject hlsInfoJson = new JSONObject();
        hlsInfoJson.put("deviceId", deviceId);
        hlsInfoJson.put("channelId", channelId);
        hlsInfoMap.put(callId, hlsInfoJson);

//		String all = "ffmpeg -i rtsp://admin:gzbbn12345@203.88.202.253:554/h264/ch1/main/av_stream -loglevel quiet -vcodec libx264 -vprofile baseline -acodec aac -ar 44100 -strict -2 -ac 1 -f flv -s 1280x720 -q 10 rtmp://127.0.0.1:1935/hls/demo";
        // 3. 启动推流
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(all);

            // 1. 保存hls推流信息
            hlsProcessMap.put(callId, process);
            deviceInfoMap.put(deviceId, process.isAlive());

            // 2. 保存基础流和分支流的关系
            PushStreamDevice pushStreamDevice = ActionController.mPushStreamDeviceManager.get(playFileName);
            String parentCallId = pushStreamDevice.getCallId();
            JSONObject subCallIdJson = ActionController.streamRelationMap.get(parentCallId);
            if (null == subCallIdJson) {
                subCallIdJson = new JSONObject();
            }
            subCallIdJson.put("hls", callId);
            ActionController.streamRelationMap.put(parentCallId, subCallIdJson);

            return callId;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 定时清理过期的ts文件
     */
    public void cleanUpTempTsFile(String deviceId, String channelId) {
        String playFileName = StreamNameUtils.play(deviceId, channelId);
        ActionController.scheduledExecutorService.scheduleAtFixedRate(() -> {
            String filePath = BaseConstants.hlsStreamPath + playFileName + "/";
            System.out.println("====================start file clean up==================");
            File dir = new File(filePath);
            File[] fileList = dir.listFiles();
            if (null != fileList) {
                TreeMap<Long, String> tm = new TreeMap<Long, String>();
                for (int i = 0; i < fileList.length; i++) {
                    Long tempLong = new Long(fileList[i].lastModified());
                    tm.put(tempLong, fileList[i].getName());
                }
                System.out.println(tm);

                Set<Long> key = tm.keySet();
                Iterator<Long> it = key.iterator();
                int i = 0;
                if (fileList.length > 15) {//文件个数大于num时候才删除
                    while (it.hasNext()) {
                        Long s = (Long) it.next();
                        if (i < (fileList).length - 15) {//删除剩下15个文件
                            String fileName = tm.get(s);
                            System.out.println(fileName);
                            new File(filePath + fileName).delete();
                        }
                        i++;
                    }
                }
            }
            System.out.println("====================finish file clean up==================");
        }, 1, 120, TimeUnit.SECONDS);
    }
}
