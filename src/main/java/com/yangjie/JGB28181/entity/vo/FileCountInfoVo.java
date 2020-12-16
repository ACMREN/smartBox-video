package com.yangjie.JGB28181.entity.vo;

import lombok.Data;

@Data
public class FileCountInfoVo {
    private String timestamp;
    private Integer snapshotCount;
    private Long snapshotSize;
    private Integer recordCount;
    private Long recordSize;
}
