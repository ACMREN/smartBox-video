package com.yangjie.JGB28181.entity.enumEntity;

public enum AlarmTypeEnum {
    MANUAL_ALARM(1, "人工报警"),
    MOVE_ALARM(2, "运动目标检测报警"),
    LEFT_ALARM(3, "遗留物检测报警"),
    OBJECT_MOVE_ALARM(4, "物体移除报警"),
    LINE_ALARM(5, "绊线检测报警"),
    INTRUSION_ALARM(6, "入侵检测报警"),
    RETROGRADE_ALARM(7, "逆行检测报警"),
    LINGER_ALARM(8, "徘徊检测报警"),
    FLOW_ALARM(9, "流量统计报警"),
    DENSITY_ALARM(10, "密度检测报警"),
    VIDEO_EXCEPTION_ALARM(11, "视频异常检测报警"),
    QUICK_MOVE_ALARM(12, "快速移动报警");

    private int code;
    private String name;

    AlarmTypeEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static AlarmTypeEnum getDataByCode(int code) {
        for (AlarmTypeEnum item : AlarmTypeEnum.values()) {
            int itemCode = item.getCode();
            if (code == itemCode) {
                return item;
            }
        }
        return null;
    }

    public static AlarmTypeEnum getDataByName(String name) {
        for (AlarmTypeEnum item : AlarmTypeEnum.values()) {
            String itemName = item.getName();
            if (name.equals(itemName)) {
                return item;
            }
        }
        return null;
    }
}
