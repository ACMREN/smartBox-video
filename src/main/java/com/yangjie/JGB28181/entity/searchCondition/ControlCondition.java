package com.yangjie.JGB28181.entity.searchCondition;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.ArrayList;
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
    private Integer deviceId;
    // 设备基础id数组
    private List<Integer> deviceIds = new ArrayList<>();
    // 控制参数
    private String direction;
    // 移动速度
    private Integer speed;
    // 云台预置点参数
    private JSONObject psConfig;
    // 预置点id列表
    private List<Integer> psIds = new ArrayList<>();
    // 放大的区域参数
    private JSONObject region;
    // 开始时间
    private String begin;
    // 结束时间
    private String end;



    private Integer pageSize;
    private Integer pageNo;
}
