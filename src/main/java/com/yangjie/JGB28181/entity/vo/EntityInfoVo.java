package com.yangjie.JGB28181.entity.vo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class EntityInfoVo {
    private Integer id;
    private String type;
    private String name;
    private JSONObject data;
}
