package com.yangjie.JGB28181.bean;

import com.yangjie.JGB28181.entity.enumEntity.HikvisionPTZCommandEnum;
import lombok.Data;

@Data
public class HikvisionControlParam extends ControlParam {
    private HikvisionPTZCommandEnum command;
}
