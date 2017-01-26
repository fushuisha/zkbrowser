package com.sand.zkb.controller;

import com.sand.zkb.service.ZKBService;
import com.sand.zkb.utils.CommonUtil;
import com.sand.zkb.utils.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ZKBController {
    @Autowired
    private ZKBService zkbService;
    @Value("${zk_servers}")
    private String zkServers;

    @RequestMapping(value = "/tree")
    @ResponseBody
    public JsonResponse tree() {
        return CommonUtil.returnJsonResponse(zkbService.tree());
    }

    @RequestMapping(value = "/readData")
    @ResponseBody
    public JsonResponse readData(String path) {
        return CommonUtil.returnJsonResponse(zkbService.readData(path));
    }

    @RequestMapping(value = "/connectZk")
    @ResponseBody
    public JsonResponse connectZK(String zkServers) {
        return CommonUtil.returnJsonResponse(zkbService.connectZK(zkServers));
    }
}
