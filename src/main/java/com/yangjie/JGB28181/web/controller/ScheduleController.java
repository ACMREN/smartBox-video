package com.yangjie.JGB28181.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.common.utils.CacheUtil;
import com.yangjie.JGB28181.entity.ScheduleFlowInfo;
import com.yangjie.JGB28181.entity.ScheduleInfo;
import com.yangjie.JGB28181.entity.vo.ScheduleInfoVo;
import com.yangjie.JGB28181.service.ScheduleFlowInfoService;
import com.yangjie.JGB28181.service.ScheduleInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Controller
@RequestMapping("/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleInfoService scheduleInfoService;

    @Autowired
    private ScheduleFlowInfoService scheduleFlowInfoService;

    /**
     * 测试流程是否符合
     * @param scheduleInfoVo
     * @return
     */
    @PostMapping("/testFlow")
    public GBResult testFlow(@RequestBody ScheduleInfoVo scheduleInfoVo) {
        String scheduleContent = scheduleInfoVo.getScheduleContent();
        scheduleInfoService.executeFlow(null, scheduleContent);

        return GBResult.ok();
    }

    /**
     * 保存流程
     * @param scheduleInfoVo
     * @return
     */
    @PostMapping("/saveFlow")
    public GBResult saveFlow(@RequestBody ScheduleInfoVo scheduleInfoVo) {
        ScheduleFlowInfo scheduleFlowInfo = new ScheduleFlowInfo(scheduleInfoVo);
        if (null == scheduleFlowInfo.getCreateTime()) {
            scheduleFlowInfo.setCreateTime(LocalDateTime.now());
        }
        scheduleFlowInfo.setUpdateTime(LocalDateTime.now());
        scheduleFlowInfoService.saveOrUpdate(scheduleFlowInfo);

        return GBResult.ok();
    }

    /**
     * 获取流程列表
     * @return
     */
    @GetMapping("/getFlowList")
    public GBResult getFlowList() {
        List<ScheduleFlowInfo> scheduleFlowInfos = scheduleFlowInfoService.list();

        return GBResult.ok(scheduleFlowInfos);
    }

    /**
     * 保存定时任务
     * @param scheduleInfoVo
     * @return
     */
    @PostMapping("/saveSchedule")
    public GBResult saveSchedule(@RequestBody ScheduleInfoVo scheduleInfoVo) {
        // 如果是更新的话，先暂停定时任务的执行
        if (null != scheduleInfoVo.getId()) {
            ScheduledFuture future = CacheUtil.scheduledFutureMap.get(scheduleInfoVo.getId());
            if (null != future) {
                future.cancel(true);
            }
        }

        ScheduleInfo scheduleInfo = new ScheduleInfo(scheduleInfoVo);
        if (null == scheduleInfo.getCreateTime()) {
            scheduleInfo.setCreateTime(LocalDateTime.now());
        }
        scheduleInfo.setUpdateTime(LocalDateTime.now());
        scheduleInfoService.saveOrUpdate(scheduleInfo);

        return GBResult.ok();
    }

    /**
     * 获取定时任务列表
     * @return
     */
    @GetMapping("/getScheduleList")
    public GBResult getScheduleList() {
        List<ScheduleInfo> resultList = scheduleInfoService.list(new QueryWrapper<ScheduleInfo>().
                select("id", "schedule_name", "schedule_desc", "schedule_cron", "create_time", "update_time"));

        return GBResult.ok(resultList);
    }

    /**
     * 执行定时任务
     * @param scheduleInfoVo
     * @return
     */
    @PostMapping("/executeSchedule")
    public GBResult executeSchedule(@RequestBody ScheduleInfoVo scheduleInfoVo) {
        Integer id = scheduleInfoVo.getId();
        ScheduleInfo scheduleInfo = scheduleInfoService.getById(id);
        String scheduleContent = scheduleInfo.getScheduleContent();
        String scheduleCron = scheduleInfo.getScheduleCron();
        scheduleInfoService.executeSchedule(id, scheduleContent, scheduleCron);
        return GBResult.ok();
    }

    /**
     * 停止定时任务
     * @param scheduleInfoVo
     * @return
     */
    @PostMapping("/stopSchedule")
    public GBResult stopSchedule(@RequestBody ScheduleInfoVo scheduleInfoVo) {
        Integer id = scheduleInfoVo.getId();
        ScheduledFuture future = CacheUtil.scheduledFutureMap.get(id);
        future.cancel(true);

        return GBResult.ok();
    }

    /**
     * 删除定时任务
     * @param scheduleInfoVo
     * @return
     */
    @PostMapping("/deleteSchedule")
    public GBResult deleteSchedule(@RequestBody ScheduleInfoVo scheduleInfoVo) {
        Integer id = scheduleInfoVo.getId();
        ScheduleInfo scheduleInfo = scheduleInfoService.getById(id);
        scheduleInfo.setIsDelete(1);
        scheduleInfoService.updateById(scheduleInfo);

        return GBResult.ok();
    }
}
