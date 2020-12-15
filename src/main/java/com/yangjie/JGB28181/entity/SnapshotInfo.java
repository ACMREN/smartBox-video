package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 截图文件信息表
 * </p>
 *
 * @author karl
 * @since 2020-12-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SnapshotInfo implements Serializable {

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
     * 缩略图路径
     */
    private String thumbnailPath;

    /**
     * 截图时间
     */
    private LocalDateTime createTime;

    /**
     * 截图类型：1-报警截图，2-AI截图
     */
    private Integer type;

    /**
     * 报警类型：1-人工视频报警； 2-运动目标检测报警； 3-遗留物检测报警； 4-物体移除检测报警； 5-绊线检测报警；6-入侵检测报警； 7-逆行检测报警； 8-徘徊检测报警； 9-流量统计报警； 10-密度检测报警； 11-视频异常检测报警； 12-快速移动报警
     */
    private Integer alarmType;


}
