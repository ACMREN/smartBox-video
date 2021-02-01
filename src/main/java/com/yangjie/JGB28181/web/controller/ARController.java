package com.yangjie.JGB28181.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.*;
import com.yangjie.JGB28181.entity.enumEntity.EntityTypeEnum;
import com.yangjie.JGB28181.entity.vo.EntityInfoVo;
import com.yangjie.JGB28181.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/AR/")
public class ARController {

    @Autowired
    private EntityInfoService entityInfoService;

    @Autowired
    private ArConfigInfoService arConfigInfoService;

    @Autowired
    private ArTagInfoService arTagInfoService;

    @Autowired
    private ArTemplateInfoService arTemplateInfoService;

    @Autowired
    private ArStyleInfoService arStyleInfoService;

    @Autowired
    private ArSceneInfoService arSceneInfoService;

    /**
     * 获取ar基础配置
     * @param deviceId
     * @return
     */
    @GetMapping("ARConfig")
    public GBResult getARConfig(@RequestParam("deviceId")Integer deviceId) {
        ArConfigInfo arConfigInfo = arConfigInfoService.getBaseMapper()
                .selectOne(new QueryWrapper<ArConfigInfo>().eq("device_base_id", deviceId));

        if (null == arConfigInfo) {
            return GBResult.ok();
        }
        return GBResult.ok(arConfigInfo.getData());
    }

    /**
     * 新增/更新ar基础配置
     * @param dataJson
     * @return
     */
    @PostMapping("ARConfig")
    public GBResult saveARConfig(@RequestBody JSONObject dataJson) {
        Integer deviceId = dataJson.getJSONObject("video").getInteger("deviceId");

        ArConfigInfo arConfigInfo = new ArConfigInfo();
        arConfigInfo.setDeviceBaseId(deviceId);
        arConfigInfo.setData(dataJson.toJSONString());

        arConfigInfoService.saveOrUpdate(arConfigInfo);

        return GBResult.ok(arConfigInfo.getData());
    }

    /**
     * 删除ar基础配置
     * @param deleteJson
     * @return
     */
    @DeleteMapping("ARConfig")
    public GBResult deleteARConfig(@RequestBody JSONObject deleteJson) {
        List<Integer> deviceIds = deleteJson.getObject("deviceIds", ArrayList.class);

        arConfigInfoService.removeByIds(deviceIds);

        return GBResult.ok();
    }

    /**
     * 获取标签列表
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("poiList")
    public GBResult getPoiList(@RequestParam("deviceId")Integer deviceId, @RequestParam("pageNo")Integer pageNo,
                               @RequestParam("pageSize")Integer pageSize) {
        Integer offset = (pageNo - 1) * pageSize;
        List<ArTagInfo> arTagInfos = arTagInfoService.getBaseMapper().selectList(new QueryWrapper<ArTagInfo>()
                .select("id", "type", "name")
                .eq("device_base_id", deviceId)
                .last("limit " + offset + ", " + pageSize));
        Integer totalCount = arTagInfoService.getBaseMapper().selectCount(null);

        PageListVo pageListVo = new PageListVo(arTagInfos, pageNo, pageSize, totalCount);

        return GBResult.ok(pageListVo);
    }

    /**
     * 获取标签列表详细数据
     * @param pIds
     * @return
     */
    @GetMapping("pois")
    public GBResult getPois(@RequestParam("ids")List<Integer> pIds) {
        List<ArTagInfo> arTagInfos = arTagInfoService.getBaseMapper().selectList(new QueryWrapper<ArTagInfo>().in("id", pIds));

        List<String> dataStrList = new ArrayList<>();
        for (ArTagInfo item : arTagInfos) {
            dataStrList.add(item.getData());
        }

        return GBResult.ok(dataStrList);
    }

    /**
     * 新增/更新标签信息
     * @param dataJson
     * @return
     */
    @PostMapping("pois")
    public GBResult savePois(@RequestBody JSONObject dataJson) {
        Integer id = dataJson.getInteger("pId");
        Integer deviceId = dataJson.getInteger("deviceId");
        String name = dataJson.getString("name");
        Integer type = dataJson.getInteger("type");

        ArTagInfo arTagInfo = new ArTagInfo();
        arTagInfo.setId(id);
        arTagInfo.setDeviceBaseId(deviceId);
        arTagInfo.setName(name);
        arTagInfo.setType(type);
        arTagInfo.setData(dataJson.toJSONString());
        arTagInfoService.saveOrUpdate(arTagInfo);

        if (null == id) {
            dataJson.put("pId", arTagInfo.getId());
            arTagInfo.setData(dataJson.toJSONString());
            arTagInfoService.saveOrUpdate(arTagInfo);
        }

        return GBResult.ok(dataJson.toJSONString());
    }

    /**
     * 删除标签信息
     * @param deleteJson
     * @return
     */
    @DeleteMapping("pois")
    public GBResult deletePois(@RequestBody JSONObject deleteJson) {
        List<Integer> pIds = deleteJson.getObject("ids", ArrayList.class);

        arTagInfoService.removeByIds(pIds);

        return GBResult.ok();
    }

    @GetMapping("poiTempList")
    public GBResult getTagStyleList(@RequestParam("pageNo")Integer pageNo, @RequestParam("pageSize")Integer pageSize) {
        Integer offset = (pageNo - 1) * pageSize;

        List<ArTemplateInfo> arTemplateInfos = arTemplateInfoService.getBaseMapper().selectList(new QueryWrapper<ArTemplateInfo>()
                .select("id", "template_name")
                .last("limit " + offset + ", " + pageSize));
        Integer totalCount = arTemplateInfoService.count();

        PageListVo pageListVo = new PageListVo(arTemplateInfos, pageNo, pageSize, totalCount);

        return GBResult.ok(pageListVo);
    }

    @GetMapping("poiTemps")
    public GBResult getTagStyle(@RequestParam("ids")List<Integer> templateIds) {
        List<ArTemplateInfo> arTemplateInfos = arTemplateInfoService.getBaseMapper().selectList(new QueryWrapper<ArTemplateInfo>()
                .in("id", templateIds));

        List<String> dataStrList = new ArrayList<>();
        for (ArTemplateInfo item : arTemplateInfos) {
            dataStrList.add(item.getData());
        }

        return GBResult.ok(dataStrList);
    }

    @PostMapping("poiTemps")
    public GBResult saveTagStyles(@RequestBody JSONObject dataJson) {
        Integer id = dataJson.getInteger("templateId");
        String name = dataJson.getString("templateName");

        ArTemplateInfo arTemplateInfo = new ArTemplateInfo();
        arTemplateInfo.setId(id);
        arTemplateInfo.setTemplateName(name);
        arTemplateInfo.setData(dataJson.toJSONString());

        arTemplateInfoService.saveOrUpdate(arTemplateInfo);

        if (null == id){
            dataJson.put("templateId", id);
            arTemplateInfo.setData(dataJson.toJSONString());
            arTemplateInfoService.saveOrUpdate(arTemplateInfo);
        }

        return GBResult.ok(arTemplateInfo.getData());
    }

    @DeleteMapping("poiTemps")
    public GBResult deleteTagStyle(@RequestBody JSONObject deleteJson) {
        List<Integer> templateIds = deleteJson.getObject("ids", ArrayList.class);

        arTemplateInfoService.removeByIds(templateIds);

        return GBResult.ok();
    }

    @GetMapping("poiStyleList")
    public GBResult getStyleList(@RequestParam("pageNo")Integer pageNo, @RequestParam("pageSize")Integer pageSize) {
        Integer offset = (pageNo - 1) * pageSize;

        List<ArStyleInfo> arStyleInfos = arStyleInfoService.getBaseMapper().selectList(new QueryWrapper<ArStyleInfo>()
                .select("id", "style_name")
                .last("limit " + offset + ", " + pageSize));
        Integer totalCount = arSceneInfoService.count();

        PageListVo pageListVo = new PageListVo(arStyleInfos, pageNo, pageSize, totalCount);

        return GBResult.ok(pageListVo);
    }

    @GetMapping("poiStyles")
    public GBResult getStyle(@RequestParam("ids")List<Integer> sIds) {
        List<ArStyleInfo> arStyleInfos = arStyleInfoService.getBaseMapper().selectList(new QueryWrapper<ArStyleInfo>()
                .in("id", sIds));

        List<String> dataStrList = new ArrayList<>();
        for (ArStyleInfo item : arStyleInfos) {
            dataStrList.add(item.getData());
        }

        return GBResult.ok(dataStrList);
    }

    @PostMapping("poiStyles")
    public GBResult saveStyle(@RequestBody JSONObject dataJson) {
        Integer id = dataJson.getInteger("sId");
        String name = dataJson.getString("styleName");

        ArStyleInfo arStyleInfo = new ArStyleInfo();
        arStyleInfo.setId(id);
        arStyleInfo.setStyleName(name);
        arStyleInfo.setData(dataJson.toJSONString());

        if (null == id){
            dataJson.put("sId", id);
            arStyleInfo.setData(dataJson.toJSONString());
            arStyleInfoService.saveOrUpdate(arStyleInfo);
        }

        return GBResult.ok(arStyleInfo.getData());
    }

    @DeleteMapping("poiStyles")
    public GBResult deleteStyle(@RequestBody JSONObject deleteJson) {
        List<Integer> sIds = deleteJson.getObject("ids", ArrayList.class);

        arStyleInfoService.removeByIds(sIds);

        return GBResult.ok();
    }

    @GetMapping("sceneList")
    public GBResult getSceneList(@RequestParam("deviceId")Integer deviceId, @RequestParam("pageNo")Integer pageNo,
                                 @RequestParam("pageSize")Integer pageSize) {
        Integer offset = (pageNo - 1) * pageSize;

        List<ArSceneInfo> arSceneInfos = arSceneInfoService.getBaseMapper().selectList(new QueryWrapper<ArSceneInfo>()
                .select("id", "name")
                .eq("device_base_id", deviceId)
                .last("limit " + offset + ", " + pageSize));
        Integer totalCount = arSceneInfoService.count();

        PageListVo pageListVo = new PageListVo(arSceneInfos, pageNo, pageSize, totalCount);

        return GBResult.ok(pageListVo);
    }

    @GetMapping("scenes")
    public GBResult getScene(@RequestParam("ids")List<Integer> sceneIds) {
        List<ArSceneInfo> arSceneInfos = arSceneInfoService.getBaseMapper().selectList(new QueryWrapper<ArSceneInfo>()
                .in("id", sceneIds));

        List<String> dataStrList = new ArrayList<>();
        for (ArSceneInfo item : arSceneInfos) {
            dataStrList.add(item.getData());
        }

        return GBResult.ok(dataStrList);
    }

    @PostMapping
    public GBResult saveScene(@RequestBody JSONObject dataJson) {
        Integer id = dataJson.getInteger("sceneId");
        Integer deviceId = dataJson.getInteger("deviceId");
        String name = dataJson.getString("name");

        ArSceneInfo arSceneInfo = new ArSceneInfo();
        arSceneInfo.setId(id);
        arSceneInfo.setDeviceBaseId(deviceId);
        arSceneInfo.setName(name);
        arSceneInfo.setData(dataJson.toJSONString());

        arSceneInfoService.saveOrUpdate(arSceneInfo);

        if (null == id){
            dataJson.put("sceneId", id);
            arSceneInfo.setData(dataJson.toJSONString());
            arSceneInfoService.saveOrUpdate(arSceneInfo);
        }

        return GBResult.ok(arSceneInfo.getData());
    }

    @DeleteMapping("scenes")
    public GBResult deleteScene(@RequestBody JSONObject deleteJson) {
        List<Integer> sceneIds = deleteJson.getObject("ids", ArrayList.class);

        arSceneInfoService.removeByIds(sceneIds);

        return GBResult.ok();
    }

    /**
     * 获取三维实体信息
     * @param type
     * @param pageNo
     * @param pageSize
     * @return
     */
    @GetMapping("/entityList")
    public GBResult getEntityList(@RequestParam("type")String type, @RequestParam("pageNo")Integer pageNo,
                              @RequestParam("pageSize")Integer pageSize) {
        Integer offset = (pageNo - 1) * pageSize;
        List<EntityInfo> entityInfos = new ArrayList<>();
        List<EntityInfoVo> entityInfoVos = new ArrayList<>();
        Integer totalCount = 0;
        if (!StringUtils.isEmpty(type)) {
            entityInfos = entityInfoService.getBaseMapper()
                    .selectList(new QueryWrapper<EntityInfo>()
                            .eq("type", type)
                            .last("limit " + offset + ", " + pageSize));
            totalCount = entityInfoService.count(new QueryWrapper<EntityInfo>().eq("type", type));
        } else {
            entityInfos = entityInfoService.getBaseMapper()
                    .selectList(new QueryWrapper<EntityInfo>()
                            .last("limit " + offset + ", " + pageSize));
            totalCount = entityInfoService.count();
        }
        if (!CollectionUtils.isEmpty(entityInfos)) {
            for (EntityInfo item : entityInfos) {
                EntityInfoVo entityInfoVo = new EntityInfoVo();
                entityInfoVo.setId(item.getId());
                entityInfoVo.setType(EntityTypeEnum.getDataByCode(item.getId()).getName());
                entityInfoVo.setName(item.getName());
                entityInfoVo.setConfig(JSONObject.parseObject(item.getData()));
                entityInfoVos.add(entityInfoVo);
            }
        }

        PageListVo pageListVo = new PageListVo(entityInfos, pageNo, pageSize, totalCount);

        return GBResult.ok(pageListVo);
    }

    @GetMapping("entitys")
    public GBResult getEntitys(@RequestParam("ids")List<Integer> ids) {
        List<EntityInfo> entityInfos = entityInfoService.listByIds(ids);

        List<String> dataStrList = new ArrayList<>();
        for (EntityInfo item : entityInfos) {
            dataStrList.add(item.getData());
        }

        return GBResult.ok(dataStrList);
    }

    /**
     * 新增/修改三维实体信息
     * @param entityInfoVo
     */
    @PostMapping("/entitys")
    public GBResult saveEntity(@RequestBody EntityInfoVo entityInfoVo) {
        Integer id = entityInfoVo.getId();
        JSONObject dataJson = entityInfoVo.getConfig();

        EntityInfo entityInfo = new EntityInfo();
        entityInfo.setId(entityInfoVo.getId());
        entityInfo.setType(EntityTypeEnum.getDataByName(entityInfoVo.getType()).getCode());
        entityInfo.setName(entityInfoVo.getName());
        entityInfo.setData(entityInfoVo.getConfig().toJSONString());
        entityInfoService.saveOrUpdate(entityInfo);

        if (null == id){
            dataJson.put("eId", id);
            entityInfo.setData(dataJson.toJSONString());
            entityInfoService.saveOrUpdate(entityInfo);
        }

        return GBResult.ok(entityInfo.getData());
    }

    @DeleteMapping("/entitys")
    public GBResult deleteEntity(@RequestBody JSONObject deleteJson) {
        List<Integer> eIds = deleteJson.getObject("eIds", ArrayList.class);

        entityInfoService.removeByIds(eIds);

        return GBResult.ok();
    }
}
