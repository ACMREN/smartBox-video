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

    DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public RecordVideoInfoVo(RecordVideoInfo recordVideoInfo) {
        this.id = recordVideoInfo.getId();
        this.deviceBaseId = recordVideoInfo.getDeviceBaseId();
        this.filePath = recordVideoInfo.getFilePath();
        this.startTime = df.format(recordVideoInfo.getStartTime());
        this.endTime = df.format(recordVideoInfo.getEndTime());
        this.fileSize = recordVideoInfo.getFileSize();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDeviceBaseId() {
        return deviceBaseId;
    }

    public void setDeviceBaseId(Integer deviceBaseId) {
        this.deviceBaseId = deviceBaseId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
}
