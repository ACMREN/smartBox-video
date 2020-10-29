package com.yangjie.JGB28181.entity.enumEntity;

public enum NetStatusEnum {
    OFFLINE(0, "offline"),
    ONLINE(1, "online");

    private int code;
    private String name;

    NetStatusEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static NetStatusEnum getDataByCode(int code) {
        for (NetStatusEnum item : NetStatusEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }
}
