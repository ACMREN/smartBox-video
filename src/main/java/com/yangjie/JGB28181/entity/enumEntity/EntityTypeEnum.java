package com.yangjie.JGB28181.entity.enumEntity;

public enum EntityTypeEnum {
    MOD(1, "mod"),
    GEO(2, "geo");

    private int code;
    private String name;

    EntityTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static EntityTypeEnum getDataByCode(int code) {
        for (EntityTypeEnum item : EntityTypeEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }

    public static EntityTypeEnum getDataByName(String name) {
        for (EntityTypeEnum item : EntityTypeEnum.values()) {
            String itemName = item.getName();
            if (name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }
}
