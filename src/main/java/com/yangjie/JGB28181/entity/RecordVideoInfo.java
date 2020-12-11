package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 录像文件信息表
 * </p>
 *
 * @author karl
 * @since 2020-12-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RecordVideoInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 基础设备id
     */
    private Integer deviceBaseId;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 文件大小
     */
    private Long fileSize;


}
