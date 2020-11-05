package com.yangjie.JGB28181.entity.enumEntity;

public enum TreeTypeEnum {
    CAMERA(0, "camera");

    private int code;
    private String name;

    TreeTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static TreeTypeEnum getDataByCode(int code) {
        for (TreeTypeEnum item : TreeTypeEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }

    public static TreeTypeEnum getDataByName(String name) {
        for (TreeTypeEnum item : TreeTypeEnum.values()) {
            String itemName = item.getName();
            if (name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }
}
