package com.yangjie.JGB28181.entity;

public enum NetTypeEnum {
    WAN(0, "WAN"),
    IT(1, "IT");

    private int code;
    private String name;

    NetTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static NetTypeEnum getDataByCode(int code) {
        for (NetTypeEnum item : NetTypeEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }
}
