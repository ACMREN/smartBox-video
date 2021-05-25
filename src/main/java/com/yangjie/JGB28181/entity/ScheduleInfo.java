package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.time.format.DateTimeFormatter;

import com.yangjie.JGB28181.entity.vo.ScheduleInfoVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 定时任务信息表
 * </p>
 *
 * @author karl
 * @since 2021-02-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ScheduleInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 定时任务名称
     */
    private String scheduleName;

    /**
     * 定时任务介绍
     */
    private String scheduleDesc;

    /**
     * 定时任务内容
     */
    private String scheduleContent;

    /**
     * 定时任务执行时间
     */
    private String scheduleCron;

    /**
     * 定时任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 定时任务更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否已经删除：0-否，1-是
     */
    private Integer isDelete;


    public ScheduleInfo() {
    }

    public ScheduleInfo(ScheduleInfoVo scheduleInfoVo) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.id = scheduleInfoVo.getId();
        this.scheduleName = scheduleInfoVo.getScheduleName();
        this.scheduleDesc = scheduleInfoVo.getScheduleDesc();
        this.scheduleContent = scheduleInfoVo.getScheduleContent();
        this.scheduleCron = scheduleInfoVo.getScheduleCron();
        if (!StringUtils.isEmpty(scheduleInfoVo.getCreateTime())) {
            this.createTime = LocalDateTime.parse(scheduleInfoVo.getCreateTime());
        }
        if (!StringUtils.isEmpty(scheduleInfoVo.getUpdateTime())) {
            this.updateTime = LocalDateTime.parse(scheduleInfoVo.getUpdateTime());
        }
    }
}
