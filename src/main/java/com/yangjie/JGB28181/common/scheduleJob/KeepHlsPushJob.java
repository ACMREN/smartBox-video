package com.yangjie.JGB28181.common.scheduleJob;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.service.impl.PushHlsStreamServiceImpl;
import com.yangjie.JGB28181.web.controller.ActionController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@EnableScheduling
public class KeepHlsPushJob {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 保持HLS推流
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void keepHlsPush() {
        logger.info("==================start check hls stream and keep save=========================");
        Map<String, Process> tempProcessMap = PushHlsStreamServiceImpl.hlsProcessMap;
        for (String processId : tempProcessMap.keySet()) {
            Process process = tempProcessMap.get(processId);
            if (!process.isAlive()) {
                // 1. 取出hls信息map中的设备信息数据
                Map<String, JSONObject> tempHlsInfoMap = PushHlsStreamServiceImpl.hlsInfoMap;
                JSONObject hlsInfoJson = tempHlsInfoMap.get(processId);
                logger.info("有hls推流进程终止，信息：processId=" + processId + ", deviceId=" + hlsInfoJson.getString("deviceId") + ", channelId=" + hlsInfoJson.getString("channelId"));

                // 2. 重启hls推流进程
                String resource = hlsInfoJson.getString("resource");
                if (!StringUtils.isEmpty(resource) && resource.equals("rtsp")) {
                    String rtspLink = hlsInfoJson.getString("rtspLink");
                    PushHlsStreamServiceImpl.pushRtspToHls(hlsInfoJson.getString("deviceId"), hlsInfoJson.getString("channelId"), rtspLink);
                } else {
                    PushHlsStreamServiceImpl.pushRtmpToHls(hlsInfoJson.getString("deviceId"), hlsInfoJson.getString("channelId"));
                }

                // 3. 删除已经终止的进程信息
                PushHlsStreamServiceImpl.hlsProcessMap.remove(processId);
                PushHlsStreamServiceImpl.hlsInfoMap.remove(processId);
            }
        }
        logger.info("==================finish check hls stream and keep save=========================");
    }
}
