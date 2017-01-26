package com.sand.zkb.utils;

import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;


public class CommonUtil {
    public static final SerializableSerializer serializableSerializer = new SerializableSerializer();

    public static String failMsg(String arg) {
        return "fail:" + StringUtils.trimToEmpty(arg);
    }

    public static JsonResponse returnJsonResponse(Object obj) {
        JsonResponse jsonResponse = new JsonResponse();
        if (obj == null) {
            jsonResponse.setCode(201);
            jsonResponse.setMsg("no data");
            return jsonResponse;
        }
        if (obj instanceof JsonResponse) {
            return (JsonResponse) obj;
        }
        if (obj instanceof String) {
            String objString = (String) obj;
            if (StringUtils.startsWith(objString, "fail")) {
                jsonResponse.setCode(201);
                jsonResponse.setMsg(objString);
                return jsonResponse;
            }
        }
        jsonResponse.setCode(200);
        jsonResponse.setMsg("success");
        jsonResponse.setData(obj);
        return jsonResponse;
    }


    public static boolean isValidCollect(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof Collection) {
            Collection<?> objl = (Collection<?>) obj;
            return !objl.isEmpty();
        } else if (obj instanceof Object[]) {
            Object[] objs = (Object[]) obj;
            return objs.length > 0;
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) > 0;
        } else if (obj instanceof Map) {
            Map<?, ?> objm = (Map<?, ?>) obj;
            return !objm.isEmpty();
        } else {
            return false;
        }
    }

    public static HttpSession getSession() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return request.getSession();
    }
}
