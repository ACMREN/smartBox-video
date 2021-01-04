package com.yangjie.JGB28181.entity.condition;

import lombok.Data;

@Data
public class SearchDeviceTreeCondition {
    /**
     * 用户id
     */
    private Integer userId;
    /**
     * 树状信息
     */
    private String treeInfo;
    /**
     * 轮询列表信息
     */
    private String pollingList;
    /**
     * 树状类型
     */
    private String treeType;
}
