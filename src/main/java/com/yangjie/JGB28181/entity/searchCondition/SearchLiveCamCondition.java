package com.yangjie.JGB28181.entity.searchCondition;

import lombok.Data;

@Data
public class SearchLiveCamCondition {
    // 连接类型
    private String linkType;
    // 网络类型
    private String netType;
    // 连接状态
    private String linkStatus;
    // 网络状态
    private String netStatus;
    // 搜索关键词
    private String keyword;
    // 排序字段
    private String sortKey;
    // 排序方式
    private String sortOrder;
    // 页码
    private Integer pageNo;
    // 每页数据
    private Integer pageSize;
}
