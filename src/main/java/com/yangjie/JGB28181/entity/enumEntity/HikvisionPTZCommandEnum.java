package com.yangjie.JGB28181.entity.enumEntity;

import com.yangjie.JGB28181.common.utils.HCNetSDK;

public enum HikvisionPTZCommandEnum {
    ZOOM_IN(HCNetSDK.ZOOM_IN, "zoomIn"),
    ZOOM_OUT(HCNetSDK.ZOOM_OUT, "zoom_out"),
    UP(HCNetSDK.TILT_UP, "up"),
    DOWN(HCNetSDK.TILT_DOWN, "down"),
    LEFT(HCNetSDK.PAN_LEFT, "left"),
    RIGHT(HCNetSDK.PAN_RIGHT, "right"),
    UP_LEFT(HCNetSDK.UP_LEFT, "upLeft"),
    UP_RIGHT(HCNetSDK.UP_RIGHT, "upRight"),
    DOWN_LEFT(HCNetSDK.DOWN_LEFT, "downLeft"),
    DOWN_RIGHT(HCNetSDK.DOWN_RIGHT, "downRight");


    private int code;
    private String name;

    HikvisionPTZCommandEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static HikvisionPTZCommandEnum getDataByCode(int code) {
        for (HikvisionPTZCommandEnum item : HikvisionPTZCommandEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }

    public static HikvisionPTZCommandEnum getDataByName(String name) {
        for (HikvisionPTZCommandEnum item : HikvisionPTZCommandEnum.values()) {
            String itemName = item.getName();
            if (name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }
}
