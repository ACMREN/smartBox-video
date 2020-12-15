package com.yangjie.JGB28181.entity.vo;

import com.yangjie.JGB28181.entity.RecordVideoInfo;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class RecordVideoInfoVo {
    private Integer id;
    private Integer deviceBaseId;
    private String filePath;
    private String startTime;
    private String endTime;
    private Long fileSize;


    public RecordVideoInfoVo(RecordVideoInfo recordVideoInfo) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.id = recordVideoInfo.getId();
        this.deviceBaseId = recordVideoInfo.getDeviceBaseId();
        this.filePath = recordVideoInfo.getFilePath();
        this.startTime = df.format(recordVideoInfo.getStartTime());
        this.endTime = df.format(recordVideoInfo.getEndTime());
        this.fileSize = recordVideoInfo.getFileSize();
    }
}
