package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.common.utils.HttpUtil;
import com.yangjie.JGB28181.entity.ScheduleDataInfo;
import com.yangjie.JGB28181.entity.ScheduleInfo;
import com.yangjie.JGB28181.mapper.ScheduleInfoMapper;
import com.yangjie.JGB28181.media.server.remux.Observer;
import com.yangjie.JGB28181.service.ScheduleDataInfoService;
import com.yangjie.JGB28181.service.ScheduleInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private ScheduleDataInfoService scheduleDataInfoService;

    static {
        threadPoolTaskScheduler.initialize();
    }

    @Override
    public void executeSchedule(Integer scheduleId, String scheduleContent, String scheduleCron) {
        ScheduledFuture future;
        // 开始执行定时任务
        future = threadPoolTaskScheduler.schedule(() -> {
            this.executeFlow(scheduleId, scheduleContent);
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
    public Map<String, Object> saveResultParam(Integer scheduleId, JSONObject resultJson, Map<String, Object> saveRuleMap) {
        List<JSONObject> saveJsonList = (List<JSONObject>) saveRuleMap.get("params");
        String category1 = (String) saveRuleMap.get("category1");
        String category2 = (String) saveRuleMap.get("category2");
        String category3 = (String) saveRuleMap.get("category3");
        String category4 = (String) saveRuleMap.get("category4");
        String category5 = (String) saveRuleMap.get("category5");

        Object category1Value = this.getDataFromParamMap(resultJson, category1);
        Object category2Value = this.getDataFromParamMap(resultJson, category2);
        Object category3Value = this.getDataFromParamMap(resultJson, category3);
        Object category4Value = this.getDataFromParamMap(resultJson, category4);
        Object category5Value = this.getDataFromParamMap(resultJson, category5);


        JSONObject saveDataJson = new JSONObject();
        for (JSONObject item : saveJsonList) {
            Set<String> keys = item.keySet();
            for (String key : keys) {
                saveDataJson.put(item.getString(key), this.getDataFromParamMap(resultJson, key));
            }
        }

        ScheduleDataInfo scheduleDataInfo = new ScheduleDataInfo();
        scheduleDataInfo.setData(saveDataJson.toJSONString());
        scheduleDataInfo.setScheduleId(scheduleId);
        scheduleDataInfo.setCategory1(category1Value==null? null : category1Value.toString());
        scheduleDataInfo.setCategory2(category2Value==null? null : category2Value.toString());
        scheduleDataInfo.setCategory3(category3Value==null? null : category3Value.toString());
        scheduleDataInfo.setCategory4(category4Value==null? null : category4Value.toString());
        scheduleDataInfo.setCategory5(category5Value==null? null : category5Value.toString());
        scheduleDataInfo.setCreateTime(LocalDateTime.now());
        scheduleDataInfo.setUpdateTime(LocalDateTime.now());
        System.out.println(scheduleDataInfo);
//        scheduleDataInfoService.saveOrUpdate(scheduleDataInfo);

        return null;
    }

    private Object getDataFromParamMap(JSONObject resultJson, String saveParamRule) {
        if (StringUtils.isEmpty(saveParamRule)) {
            return null;
        }

        Object data = null;
        String[] fieldArr = saveParamRule.split("\\.");
        int length = fieldArr.length;

        data = this.getDataByRecursion(resultJson, fieldArr, 0, length);
        System.out.println(data);
        return data;
    }

    private Object getDataByRecursion(Object resultObject, String[] fieldArr, int i, int length) {
         String field = fieldArr[i];
         if (i == length - 1) {
            return ((JSONObject) resultObject).get(field);
        }
        if (field.contains("[]")) {
            JSONObject resultJson = (JSONObject) resultObject;
            field = field.replace("[]", "");
            List<JSONObject> jsonList = resultJson.getObject(field, ArrayList.class);
            List<Object> dataList = new ArrayList<>();
            i++;
            for (JSONObject json : jsonList) {
                dataList.add(this.getDataByRecursion(json, fieldArr, i, length));
            }
            return dataList;
        } else {
            JSONObject dataJson = ((JSONObject) resultObject).getJSONObject(field);
            return this.getDataByRecursion(dataJson, fieldArr, ++i, length);
        }
    }

    @Override
    public void executeFlow(Integer scheduleId, String scheduleContent) {
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
                if (null != scheduleId) {
                    if (content.containsKey("save")) {
                        JSONObject saveJson = content.getJSONObject("save");
                        Map<String, Object> saveRuleMap = JSONObject.parseObject(saveJson.toJSONString(), HashMap.class);
                        JSONObject resultJson = new JSONObject(resultMap);
                        this.saveResultParam(scheduleId, resultJson, saveRuleMap);
                    }
                }
            }
            if (content.containsKey("output")) {
                JSONObject saveJson = content.getJSONObject("output");
            }
        }
        logger.info(resultMap.toString());
    }
}
