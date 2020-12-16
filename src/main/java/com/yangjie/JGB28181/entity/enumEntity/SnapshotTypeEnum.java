package com.yangjie.JGB28181.entity.enumEntity;

public enum SnapshotTypeEnum {
    ALARM_SNAPSHOT(1, "alarmSnapshot"),
    AI_SNAPSHOT(2, "aiSnapshot"),
    MANUAL_SNAPSHOT(3, "manualSnapshot");

    private int code;
    private String name;

    SnapshotTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static SnapshotTypeEnum getDataByCode(int code) {
        for (SnapshotTypeEnum item : SnapshotTypeEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }

    public static SnapshotTypeEnum getDataByName(String name) {
        for (SnapshotTypeEnum item : SnapshotTypeEnum.values()) {
            String itemName = item.getName();
            if (name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }
}
