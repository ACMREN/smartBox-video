package com.yangjie.JGB28181.web.controller;

import com.yangjie.JGB28181.entity.EntityInfo;
import com.yangjie.JGB28181.service.EntityInfoService;
import org.springframework.beans.factory.annotation.Autowired;
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
                          @RequestParam("pageSize")Integer pageSize, @RequestParam("sid")List<String> sid) {
        System.out.println(type);
        System.out.println(pageNo);
        System.out.println(pageSize);
        System.out.println(sid);
    }

    @PostMapping("/entitys")
    public void saveEntity(@RequestBody EntityInfo entityInfo) {
        System.out.println(entityInfo);
    }



}
