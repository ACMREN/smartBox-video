package com.yangjie.JGB28181.entity;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * ar基础设置表
 * </p>
 *
 * @author karl
 * @since 2021-01-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ArConfigInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备基础id
     */
    @TableId("device_base_id")
    private Integer deviceBaseId;

    /**
     * 数据
     */
    private String data;


}
