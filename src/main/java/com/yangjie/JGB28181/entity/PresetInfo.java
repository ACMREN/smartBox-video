package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 预置点信息表
 * </p>
 *
 * @author karl
 * @since 2020-12-10
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PresetInfo implements Serializable {

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
     * 预置点名称
     */
    private String presetName;

    /**
     * 预置点点位信息
     */
    private String presetPos;


}
