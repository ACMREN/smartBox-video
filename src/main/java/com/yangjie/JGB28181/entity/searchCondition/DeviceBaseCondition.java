package com.yangjie.JGB28181.entity.searchCondition;

import lombok.Data;

import java.util.List;

@Data
public class DeviceBaseCondition {
    private List<Integer> deviceId;
    // 推流方式
    private String type;
    // 是否开启推流
    private Integer isSwitch;
    // 兼容参数
    private List<Integer> deviceIds;
}
