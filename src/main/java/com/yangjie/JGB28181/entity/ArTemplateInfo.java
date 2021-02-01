package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * ar标签模板表
 * </p>
 *
 * @author karl
 * @since 2021-01-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ArTemplateInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 标签模板名称
     */
    private String templateName;

    /**
     * 数据
     */
    private String data;


}
