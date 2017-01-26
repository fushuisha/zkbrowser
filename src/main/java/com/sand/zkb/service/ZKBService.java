package com.sand.zkb.service;

import com.sand.zkb.utils.CommonUtil;
import com.sand.zkb.utils.ConstUtil;
import com.sand.zkb.utils.ZKNode;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by david.sha on 2017/1/26.
 */
@Service
public class ZKBService {
    private final Logger logger = LoggerFactory.getLogger(ZKBService.class);
    @Value("${zk_servers}")
    private String zkServers;

    public Object tree() {

        /**
         * 创建会话
         * new SerializableSerializer() 创建序列化器接口，用来序列化和反序列化
         */
//        SerializableSerializer serializableSerializer = new SerializableSerializer();
        ZkClient zkClient = getZkClient();
        if (zkClient == null) {
            return CommonUtil.failMsg("can't connect!");
        }
//        Map<String, Object> map = new HashMap<>();
//        map.put("/", "/");
        ZKNode root = new ZKNode();
        root.setRealPath("/");
        root.setViewPath("/");
        List<ZKNode> list = new ArrayList<>();
        list.add(root);
        zktree(root, zkClient, "/", list);
        return list;
    }

    private String getZkServers() {
        //zk集群的地址
        HttpSession httpSession = CommonUtil.getSession();
        String zkServers = (String) httpSession.getAttribute(ConstUtil.ZK_SERVERS);
        if (StringUtils.isBlank(zkServers)) {
            zkServers = this.zkServers;
        }
        httpSession.setAttribute(ConstUtil.ZK_SERVERS, zkServers);
        return zkServers;
    }

    private ZkClient getZkClient() {
        try {
            ZkClient zkClient = new ZkClient(getZkServers(), 10000, 10000);
            return zkClient;
        } catch (Exception ex) {
            return null;
        }
    }

    private void zktree(ZKNode parent, ZkClient zkClient, String nodePath, List<ZKNode> list) {
        try {
            List<String> children = zkClient.getChildren(nodePath);
            if (CommonUtil.isValidCollect(children)) {
                for (String path : children) {
                    if (StringUtils.equals("/", nodePath)) {
                        path = nodePath + path;
                    } else {
                        path = nodePath + "/" + path;
                    }
                    ZKNode zkNode = new ZKNode();
                    zkNode.setRealPath(path);
//                    zkNode.setViewPath(genViewPath(path));
//                    zkNode.setBytesValue(zkClient.readDataBytes(path));
                    list.add(zkNode);
                    parent.getChildren().add(zkNode);
                    zktree(zkNode, zkClient, path, list);
                }
            }
        } catch (Exception ex) {
            logger.warn(ex.toString(), ex);
        }
    }

    public Object readData(String path) {
        ZkClient zkClient = getZkClient();
        if (zkClient == null) {
            return CommonUtil.failMsg("can't connect!");
        }
        ZKNode zkNode = new ZKNode();
        zkNode.setRealPath(path);
        zkNode.setBytesValue(zkClient.readDataBytes(path));
        return zkNode;
    }

    public Object connectZK(String zkServers) {
        zkServers = StringUtils.trimToEmpty(zkServers);
        try {
            if (StringUtils.isBlank(zkServers)) {
                return CommonUtil.failMsg("can't connect!");
            }
            zkServers = zkServers.replace("：",":").replace("，",",");
            new ZkClient(zkServers, 10000, 10000);
            CommonUtil.getSession().setAttribute(ConstUtil.ZK_SERVERS, zkServers);
            return "success";
        } catch (Exception ex) {
            return CommonUtil.failMsg("can't connect!");
        }
    }
}
