package com.yangjie.JGB28181.entity.enumEntity;

public enum LinkStatusEnum {
    REGISTERED(1, "registered"),
    UNREGISTERED(0, "unregistered");

    private int code;
    private String name;

    LinkStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static LinkStatusEnum getDataByCode(int code) {
        for (LinkStatusEnum item : LinkStatusEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }
}
