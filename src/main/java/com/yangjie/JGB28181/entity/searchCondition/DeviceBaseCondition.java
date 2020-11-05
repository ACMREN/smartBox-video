package com.yangjie.JGB28181.entity.searchCondition;

import lombok.Data;

import java.util.List;

@Data
public class DeviceBaseCondition {
    private List<Integer> deviceId;
    // 推流方式
    private String type;
}
