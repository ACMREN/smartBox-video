package com.yangjie.JGB28181.entity.searchCondition;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class ControlCondition {
    /****************  对未注册设备进行控制  ****************/
    private String producer;
    private String ip;
    private Integer port;
    private String userName;
    private String password;
    private JSONObject PTZParams;

    /****************  对已注册设备进行控制  ****************/
    // 设备基础id
    private List<Integer> deviceId;
    // 控制参数
    private JSONObject controls;
    // 是否停止
    private Integer isStop;
}
