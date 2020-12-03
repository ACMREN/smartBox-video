package com.yangjie.JGB28181.entity.searchCondition;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class ControlCondition {
    private String ip;
    private Integer port;
    private String userName;
    private String password;
    private List<JSONObject> PTZParams;
}
