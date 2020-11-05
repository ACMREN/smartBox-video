package com.yangjie.JGB28181.entity.enumEntity;

public enum LinkTypeEnum {
    RTSP(0, "rtsp"),
    GB28181(1, "gb28181"),
    PLATFORM(2, "platform");

    private int code;
    private String name;

    LinkTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static LinkTypeEnum getDataByCode(int code) {
        for (LinkTypeEnum item : LinkTypeEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }

    public static LinkTypeEnum getDataByName(String name) {
        for (LinkTypeEnum item : LinkTypeEnum.values()) {
            String itemName = item.getName();
            if (name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }
}
