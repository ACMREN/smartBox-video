package com.yangjie.JGB28181.entity.vo;

import lombok.Data;

@Data
public class ScheduleInfoVo {
    private Integer id;
    private String flowName;
    private String scheduleName;
    private String scheduleDesc;
    private String scheduleContent;
    private String scheduleCron;
    private String createTime;
    private String updateTime;
}
