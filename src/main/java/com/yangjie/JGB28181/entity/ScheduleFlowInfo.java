package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.yangjie.JGB28181.entity.vo.ScheduleInfoVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 定时任务流程信息表
 * </p>
 *
 * @author karl
 * @since 2021-02-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ScheduleFlowInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 定时任务流程名称
     */
    private String flowName;

    /**
     * 定时任务流程内容
     */
    private String flowContent;

    /**
     * 流程创建时间
     */
    private LocalDateTime createTime;

    /**
     * 流程更新时间
     */
    private LocalDateTime updateTime;

    public ScheduleFlowInfo() {
    }

    public ScheduleFlowInfo(ScheduleInfoVo scheduleInfoVo) {
        this.id = scheduleInfoVo.getId();
        this.flowName = scheduleInfoVo.getFlowName();
        this.flowContent = scheduleInfoVo.getScheduleContent();
        if (!StringUtils.isEmpty(scheduleInfoVo.getCreateTime())) {
            this.createTime = LocalDateTime.parse(scheduleInfoVo.getCreateTime());
        }
        if (!StringUtils.isEmpty(scheduleInfoVo.getUpdateTime())) {
            this.updateTime = LocalDateTime.parse(scheduleInfoVo.getUpdateTime());
        }
    }
}
