package com.sand.zkb.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ZKNode {
    private String realPath;
    private String viewPath;
    private byte[] bytesValue;
    private Object object;
    private String stringValue;
    private String objectValue;
    private String hexValue;
    private List<ZKNode> children = new ArrayList<>();

    public String getRealPath() {
        return realPath;
    }

    public void setRealPath(String realPath) {
        this.realPath = realPath;
    }

    public String getViewPath() {
        if (StringUtils.isNotBlank(this.viewPath)) {
            return this.viewPath;
        } else {
            if (StringUtils.isNotBlank(this.realPath)) {
                return genViewPath(this.realPath);
            } else {
                return null;
            }
        }
    }

    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }

    public byte[] getBytesValue() {
        return bytesValue;
    }

    public void setBytesValue(byte[] bytesValue) {
        this.bytesValue = bytesValue;
    }

    public String getStringValue() {
        if (StringUtils.isNotBlank(this.stringValue)) {
            return this.stringValue;
        } else {
            byte[] bytes = getBytesValue();
            if (bytes != null) {
                try {
                    return new String(bytes, ConstUtil.UTF8);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getObjectValue() {
        if (StringUtils.isNotBlank(this.objectValue)) {
            return objectValue;
        } else {
            Object obj = getObject();
            if (obj != null) {
                return obj.toString();
            } else {
                return null;
            }
        }
    }

    public void setObjectValue(String objectValue) {
        this.objectValue = objectValue;
    }

    public String getHexValue() {
        if (StringUtils.isNotBlank(this.hexValue)) {
            return this.hexValue;
        } else {
            byte[] bytes = getBytesValue();
            if (bytes != null) {
                try {
                    return Hex.encodeHexString(bytes);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public void setHexValue(String hexValue) {
        this.hexValue = hexValue;
    }

    public List<ZKNode> getChildren() {
        return children;
    }

    public void setChildren(List<ZKNode> children) {
        this.children = children;
    }

    public Object getObject() {
        if (this.object != null) {
            return this.object;
        } else {
            byte[] bytes = getBytesValue();
            if (bytes != null) {
                try {
                    return CommonUtil.serializableSerializer.deserialize(bytes);
                } catch (Exception ex) {
//                    ex.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public void setObject(Object object) {
        this.object = object;
    }

    private String genViewPath(String path) {
        String[] splitArray = StringUtils.split(path, "/");
        if (CommonUtil.isValidCollect(splitArray)) {
            path = "";
            for (int i = 0; i < splitArray.length; i++) {
                if (i == splitArray.length - 1) {
                    String split = splitArray[i];
                    path += split;
                } else {
                    path += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                }
            }
        }
        return path;
    }
}
