package com.yangjie.JGB28181.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 树状图表
 * </p>
 *
 * @author karl
 * @since 2020-11-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TreeInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户id
     */
    private Integer userId;


    /**
     * 树状图信息
     */
    private String treeInfo;

    /**
     * 轮询列表信息
     */
    private String pollingList;

    /**
     * 树状图类型：0-摄像头
     */
    private Integer treeType;


}
