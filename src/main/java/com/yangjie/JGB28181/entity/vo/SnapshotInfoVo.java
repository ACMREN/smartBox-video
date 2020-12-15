package com.yangjie.JGB28181.entity.vo;

import com.yangjie.JGB28181.entity.SnapshotInfo;
import com.yangjie.JGB28181.entity.enumEntity.AlarmTypeEnum;
import com.yangjie.JGB28181.entity.enumEntity.SnapshotTypeEnum;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class SnapshotInfoVo {
    private Integer id;
    private Integer deviceBaseId;
    private String filePath;
    private Long fileSize;
    private String thumbnailPath;
    private String createTime;
    private String type;
    private String alarmType;

    private DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SnapshotInfoVo(SnapshotInfo snapshotInfo) {
        this.id = snapshotInfo.getId();
        this.deviceBaseId = snapshotInfo.getDeviceBaseId();
        this.filePath = snapshotInfo.getFilePath();
        this.fileSize = snapshotInfo.getFileSize();
        this.thumbnailPath = snapshotInfo.getThumbnailPath();
        this.createTime = df.format(snapshotInfo.getCreateTime());
        this.type = SnapshotTypeEnum.getDataByCode(snapshotInfo.getType()).getName();
        this.alarmType = AlarmTypeEnum.getDataByCode(snapshotInfo.getAlarmType()).getName();
    }
}
