package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.common.utils.HttpUtil;
import com.yangjie.JGB28181.entity.ScheduleInfo;
import com.yangjie.JGB28181.mapper.ScheduleInfoMapper;
import com.yangjie.JGB28181.service.ScheduleInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * <p>
 * 定时任务信息表 服务实现类
 * </p>
 *
 * @author karl
 * @since 2021-02-25
 */
@Service
public class ScheduleInfoServiceImpl extends ServiceImpl<ScheduleInfoMapper, ScheduleInfo> implements ScheduleInfoService {
    private static Logger logger = LoggerFactory.getLogger(ScheduleInfoServiceImpl.class);

    private static ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

    static {
        threadPoolTaskScheduler.initialize();
    }

    @Override
    public void executeSchedule(Integer scheduleId, String scheduleContent, String scheduleCron) {
        ScheduledFuture future;
        // 开始执行定时任务
        future = threadPoolTaskScheduler.schedule(() -> {
            this.executeFlow(scheduleContent);
        }, new CronTrigger(scheduleCron));

        // 加入到map中进行管理
        CacheUtil.scheduledFutureMap.put(scheduleId, future);
    }

    @Override
    public Map<String, Object> requestUrl(String url, String method, Map<String, Object> paramMap) {
        String result = "";
        Map<String, Object> resultMap = new HashMap<>();
        if (method.toUpperCase().equals("GET")) {
            result = HttpUtil.doGet(url, paramMap);
        }
        if (method.toUpperCase().equals("POST")) {
            result = HttpUtil.doPost(url, paramMap);
        }
        resultMap = JSONObject.parseObject(result, HashMap.class);

        return resultMap;
    }

    @Override
    public Map<String, Object> verifyResultParam(JSONObject resultJson, Map<String, Object> resultParamRuleMap) {
        return null;
    }

    @Override
    public Map<String, Object> saveResultParam(Map<String, Object> saveParamMap) {
        return null;
    }

    @Override
    public void executeFlow(String scheduleContent) {
        List<JSONObject> scheduleList = JSONObject.parseArray(scheduleContent, JSONObject.class);
        Map<String, Object> resultMap = new HashMap<>();
        for (JSONObject content : scheduleList) {
            if (content.containsKey("api")) {
                JSONObject apiJson = content.getJSONObject("api");
                String url = apiJson.getString("url");
                String method = apiJson.getString("method");
                String paramStr = apiJson.getString("params");
                Map<String, Object> paramMap = JSONObject.parseObject(paramStr, HashMap.class);
                resultMap.putAll(this.requestUrl(url, method, paramMap));
            }
            if (content.containsKey("verify")) {

            }
            if (content.containsKey("save")) {

            }
        }
        logger.info(resultMap.toString());
    }
}
