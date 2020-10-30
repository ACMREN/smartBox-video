package com.yangjie.JGB28181.entity.enumEntity;

public enum DeviceLinkEnum {
    WIRE(0, "wire"),
    WIRELESS(1, "wireless");

    private int code;
    private String name;

    DeviceLinkEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static DeviceLinkEnum getDataByCode(int code) {
        for (DeviceLinkEnum item : DeviceLinkEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }

    public static DeviceLinkEnum getDataByName(String name) {
        for (DeviceLinkEnum item : DeviceLinkEnum.values()) {
            String itemName = item.getName();
            if (name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }
}
