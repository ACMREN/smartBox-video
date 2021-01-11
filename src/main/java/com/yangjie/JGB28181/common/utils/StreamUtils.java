package com.yangjie.JGB28181.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class StreamUtils {

    public static String getStreamMediaIp() throws IOException {
        File file = ResourceUtils.getFile("classpath:config.properties");
        InputStream in = new FileInputStream(file);
        Properties properties = new Properties();
        properties.load(in);
        String streamMediaIp = properties.getProperty("config.streamMediaIp");
        return streamMediaIp;
    }

    /**
     * 定时清理过期的ts文件
     */
    public static void cleanUpTempTsFile(String deviceId, String channelId, Integer isRtsp) {
        String playFileName = null;
        if (isRtsp == 1) {
            playFileName = StreamNameUtils.rtspPlay(deviceId, channelId);
        } else {
            playFileName = StreamNameUtils.play(deviceId, channelId);
        }
        final String documentName = playFileName;
        ScheduledFuture<?> scheduledFuture = CacheUtil.scheduledExecutorService.scheduleAtFixedRate(() -> {
            String filePath = BaseConstants.hlsStreamPath + documentName + "/";
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

    public static void handleStreamInfoMap(String callId, Integer deviceId, String type) {
        // 设置关闭推流的标志位为假
        CacheUtil.callEndMap.put(callId, false);

        // 每请求一次对应的推流，则观看人数加一
        Integer count = CacheUtil.callIdCountMap.get(callId);
        if (null == count) {
            count = 1;
        } else {
            count++;
        }
        CacheUtil.callIdCountMap.put(callId, count);

        // 把推流的信息放入设备callId的map中
        JSONObject streamJson = CacheUtil.baseDeviceIdCallIdMap.get(deviceId);
        if (null == streamJson) {
            streamJson = new JSONObject();
        }
        JSONObject typeStreamJson = new JSONObject();
        typeStreamJson.put("callId", callId);
        streamJson.put(type, typeStreamJson);
        CacheUtil.baseDeviceIdCallIdMap.put(deviceId, streamJson);
    }
}
