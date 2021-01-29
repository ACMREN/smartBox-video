package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 三维实体数据表
 * </p>
 *
 * @author karl
 * @since 2021-01-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EntityInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 三维实体类型：1-模型，2-多边形
     */
    private Integer type;

    /**
     * 三维实体名称
     */
    private String name;

    /**
     * 数据
     */
    private String data;


}
