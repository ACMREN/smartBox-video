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
    private String remark;
    private String alarmType;


    public SnapshotInfoVo(SnapshotInfo snapshotInfo) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.id = snapshotInfo.getId();
        this.deviceBaseId = snapshotInfo.getDeviceBaseId();
        this.filePath = snapshotInfo.getFilePath();
        this.fileSize = snapshotInfo.getFileSize();
        this.thumbnailPath = snapshotInfo.getThumbnailPath();
        this.createTime = df.format(snapshotInfo.getCreateTime());
        this.remark = SnapshotTypeEnum.getDataByCode(snapshotInfo.getType()).getName();
        if (snapshotInfo.getAlarmType().intValue() != 0) {
            this.alarmType = AlarmTypeEnum.getDataByCode(snapshotInfo.getAlarmType()).getName();
        }
    }
}
