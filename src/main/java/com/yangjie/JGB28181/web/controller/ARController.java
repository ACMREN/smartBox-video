package com.yangjie.JGB28181.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.ArConfigInfo;
import com.yangjie.JGB28181.entity.EntityInfo;
import com.yangjie.JGB28181.entity.enumEntity.EntityTypeEnum;
import com.yangjie.JGB28181.entity.vo.EntityInfoVo;
import com.yangjie.JGB28181.service.ArConfigInfoService;
import com.yangjie.JGB28181.service.EntityInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/AR/")
public class ARController {

    @Autowired
    private EntityInfoService entityInfoService;

    @Autowired
    private ArConfigInfoService arConfigInfoService;

    /**
     * 获取ar基础配置
     * @param deviceId
     * @return
     */
    @GetMapping("ARConfig")
    public GBResult getARConfig(@RequestParam("deviceId")Integer deviceId) {
        ArConfigInfo arConfigInfo = arConfigInfoService.getBaseMapper()
                .selectOne(new QueryWrapper<ArConfigInfo>().eq("device_base_id", deviceId));

        return GBResult.ok(arConfigInfo);
    }

    /**
     * 新增/更新ar基础配置
     * @param arConfigInfo
     * @return
     */
    @PostMapping("ARConfig")
    public GBResult saveARConfig(@RequestBody ArConfigInfo arConfigInfo) {
        arConfigInfoService.save(arConfigInfo);

        return GBResult.ok();
    }

    /**
     * 删除ar基础配置
     * @param deleteJson
     * @return
     */
    @DeleteMapping("ARConfig")
    public GBResult deleteARConfig(@RequestBody JSONObject deleteJson) {
        List<Integer> arIds = deleteJson.getObject("arIds", ArrayList.class);

        for (Integer item : arIds) {
            arConfigInfoService.removeById(item);
        }

        return GBResult.ok();
    }

    /**
     * 获取三维实体信息
     * @param type
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/entitys")
    public GBResult getEntity(@RequestParam("type")String type, @RequestParam("pageNo")Integer pageNo,
                              @RequestParam("pageSize")Integer pageSize) {
        Integer offset = (pageNo - 1) * pageSize;
        List<EntityInfo> entityInfos = new ArrayList<>();
        List<EntityInfoVo> entityInfoVos = new ArrayList<>();
        if (!StringUtils.isEmpty(type)) {
            entityInfos = entityInfoService.getBaseMapper()
                    .selectList(new QueryWrapper<EntityInfo>()
                            .eq("type", type)
                            .last("limit " + offset + ", " + pageSize));
        } else {
            entityInfos = entityInfoService.getBaseMapper()
                    .selectList(new QueryWrapper<EntityInfo>()
                            .last("limit " + offset + ", " + pageSize));
        }
        if (!CollectionUtils.isEmpty(entityInfos)) {
            for (EntityInfo item : entityInfos) {
                EntityInfoVo entityInfoVo = new EntityInfoVo();
                entityInfoVo.setId(item.getId());
                entityInfoVo.setType(EntityTypeEnum.getDataByCode(item.getId()).getName());
                entityInfoVo.setName(item.getName());
                entityInfoVo.setData(item.getData());
                entityInfoVos.add(entityInfoVo);
            }
        }
        return GBResult.ok(entityInfoVos);
    }

    /**
     * 新增/修改三维实体信息
     * @param entityInfoVo
     */
    @PostMapping("/entitys")
    public GBResult saveEntity(@RequestBody EntityInfoVo entityInfoVo) {
        EntityInfo entityInfo = new EntityInfo();
        entityInfo.setId(entityInfoVo.getId());
        entityInfo.setType(EntityTypeEnum.getDataByName(entityInfo.getName()).getCode());
        entityInfo.setName(entityInfoVo.getName());
        entityInfo.setData(entityInfo.getData());
        entityInfoService.saveOrUpdate(entityInfo);

        return GBResult.ok(entityInfo.getId());
    }

    @DeleteMapping("/entitys")
    public GBResult deleteEntity(@RequestBody JSONObject deleteJson) {
        List<Integer> eIds = deleteJson.getObject("eIds", ArrayList.class);
        for (Integer id : eIds) {
            entityInfoService.removeById(id);
        }

        return GBResult.ok();
    }
}
