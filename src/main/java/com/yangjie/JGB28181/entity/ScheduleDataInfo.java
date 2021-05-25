package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 定时任务保存数据信息表
 * </p>
 *
 * @author karl
 * @since 2021-03-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ScheduleDataInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 保存的数据
     */
    private String data;

    /**
     * 定时任务id
     */
    private Integer scheduleId;

    /**
     * 类别1
     */
    private String category1;

    /**
     * 类别2
     */
    private String category2;

    /**
     * 类别3
     */
    private String category3;

    /**
     * 类别4
     */
    private String category4;

    /**
     * 类别5
     */
    private String category5;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
