package com.yangjie.JGB28181.entity.searchCondition;

import lombok.Data;

@Data
public class SearchLiveCamCondition {
    private String linkType;
    private String netType;
    private String linkStatus;
    private String netStatus;
    private String keyword;
    private String sortKey;
    private String sortOrder;
    private Integer pageNo;
    private Integer pageSize;
}
