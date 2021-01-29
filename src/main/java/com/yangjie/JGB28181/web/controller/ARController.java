package com.yangjie.JGB28181.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.entity.EntityInfo;
import com.yangjie.JGB28181.entity.enumEntity.EntityTypeEnum;
import com.yangjie.JGB28181.entity.vo.EntityInfoVo;
import com.yangjie.JGB28181.service.EntityInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;
import java.util.List;

@RestController
@RequestMapping("/AR/")
public class ARController {

    @Autowired
    private EntityInfoService entityInfoService;

    @GetMapping("/entitys")
    public void getEntity(@RequestParam("type")String type, @RequestParam("pageNo")Integer pageNo,
                          @RequestParam("pageSize")Integer pageSize) {
        Integer offset = (pageNo - 1) * pageSize;
        if (!StringUtils.isEmpty(type)) {
            entityInfoService.getBaseMapper().selectList(new QueryWrapper<EntityInfo>().eq("type", type).last("limit " + ))
        }
    }

    @PostMapping("/entitys")
    public void saveEntity(@RequestBody EntityInfoVo entityInfoVo) {
        EntityInfo entityInfo = new EntityInfo();
        entityInfo.setId(entityInfoVo.getId());
        entityInfo.setType(EntityTypeEnum.getDataByName(entityInfo.getName()).getCode());
        entityInfo.setName(entityInfoVo.getName());
        entityInfo.setData(entityInfo.getData());
        entityInfoService.saveOrUpdate(entityInfo);
    }



}
