package com.yangjie.JGB28181.service;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.entity.ScheduleInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 定时任务信息表 服务类
 * </p>
 *
 * @author karl
 * @since 2021-02-25
 */
public interface ScheduleInfoService extends IService<ScheduleInfo> {

    /**
     * 执行定时任务
     * @param scheduleId
     * @param scheduleContent
     * @param scheduleCron
     */
    void executeSchedule(Integer scheduleId, String scheduleContent, String scheduleCron);

    /**
     * 发起api请求获取数据
     * @param url
     * @param method
     * @param paramMap
     * @return
     */
    Map<String, Object> requestUrl(String url, String method, Map<String, Object> paramMap);

    /**
     * 验证获取数据的正确性
     * @param resultJson
     * @param resultParamRuleMap
     * @return
     */
    Map<String, Object> verifyResultParam(JSONObject resultJson, Map<String, Object> resultParamRuleMap);

    /**
     * 保存入库
     * @param resultJson
     * @return
     */
    Map<String, Object> saveResultParam(Integer scheduleId, JSONObject resultJson, Map<String, Object> saveRuleMap);

    void executeFlow(Integer scheduleId, String scheduleContent);
}
