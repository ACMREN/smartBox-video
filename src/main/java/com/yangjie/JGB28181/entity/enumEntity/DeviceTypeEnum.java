package com.yangjie.JGB28181.entity.enumEntity;

public enum DeviceTypeEnum {
    CAMERA_GUN(1, "camera_gun"),
    CAMERA_BALL(2, "camera_ball"),
    CAMERA_HALFBALL(3, "camera_halfball");

    private int code;
    private String name;

    DeviceTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static DeviceTypeEnum getDataByCode(int code) {
        for (DeviceTypeEnum item : DeviceTypeEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }

    public static DeviceTypeEnum getDataByName(String name) {
        for (DeviceTypeEnum item : DeviceTypeEnum.values()) {
            String itemName = item.getName();
            if (name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }
}
