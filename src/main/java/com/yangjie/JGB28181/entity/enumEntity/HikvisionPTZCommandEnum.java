package com.yangjie.JGB28181.entity.enumEntity;

import com.yangjie.JGB28181.common.utils.HCNetSDK;

public enum HikvisionPTZCommandEnum {
    ZOOM_IN(HCNetSDK.ZOOM_IN, "ZOOM_IN"),
    ZOOM_OUT(HCNetSDK.ZOOM_OUT, "ZOOM_OUT"),
    UP(HCNetSDK.TILT_UP, "TILT_UP"),
    DOWN(HCNetSDK.TILT_DOWN, "TILT_DOWN"),
    LEFT(HCNetSDK.PAN_LEFT, "PAN_LEFT"),
    RIGHT(HCNetSDK.PAN_RIGHT, "PAN_RIGHT"),
    UP_LEFT(HCNetSDK.UP_LEFT, "UP_LEFT"),
    UP_RIGHT(HCNetSDK.UP_RIGHT, "UP_RIGHT"),
    DOWN_LEFT(HCNetSDK.DOWN_LEFT, "DOWN_LEFT"),
    DOWN_RIGHT(HCNetSDK.DOWN_RIGHT, "DOWN_RIGHT");


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
