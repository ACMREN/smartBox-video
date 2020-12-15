package com.yangjie.JGB28181.common.utils;

import com.yangjie.JGB28181.web.controller.DeviceManagerController;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class RecordNameUtils {
    private static Calendar calendar = Calendar.getInstance();

    public static String recordVideoFileAddress(String streamName) {
        String baseRecordDir = DeviceManagerController.cameraConfigBo.getRecordDir();
        String dateDir = String.valueOf(calendar.get(Calendar.YEAR)) + String.valueOf(calendar.get(Calendar.MONTH) + 1) + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        String recordDir = baseRecordDir + "/" + streamName + "/" + dateDir + "/";
        File filePath = new File(recordDir);
        // 判断文件夹路径是否存在，不存在则创建
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        String recordFileName = getRecordFileName(recordDir);

        return recordDir + recordFileName;
    }

    private static String getRecordFileName(String filePath) {
        calendar.setTime(new Date());

        String fileName = "record_" + calendar.getTimeInMillis() + ".flv";
        String recordAddress = filePath + fileName;
        File recordFile = new File(recordAddress);
        if (recordFile.exists()) {
            getRecordFileName(filePath);
        }
        return fileName;
    }

    /**
     * 获取截图地址
     * @param streamName
     * @return
     */
    public static String snapshotFileAddress(String streamName) {
        String baseRecordDir = DeviceManagerController.cameraConfigBo.getSnapShootDir();
        String dateDir = String.valueOf(calendar.get(Calendar.YEAR)) + String.valueOf(calendar.get(Calendar.MONTH) + 1) + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        String snapshotDir = baseRecordDir + "/" + streamName + "/" + dateDir + "/";
        File filePath = new File(snapshotDir);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        calendar.setTime(new Date());
        String snapshotName = "snap_" + calendar.getTimeInMillis() + ".jpg";

        return snapshotDir + snapshotName;
    }

    /**
     * 获取缩略图地址
     * @param streamName
     * @return
     */
    public static String thumbnailFileAddress(String streamName) {
        String baseRecordDir = DeviceManagerController.cameraConfigBo.getSnapShootDir();
        String dateDir = String.valueOf(calendar.get(Calendar.YEAR)) + String.valueOf(calendar.get(Calendar.MONTH) + 1) + String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        String snapshotDir = baseRecordDir + "/" + streamName + "/" + dateDir + "/" + "thumb" + "/";
        File filePath = new File(snapshotDir);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        calendar.setTime(new Date());
        String snapshotName = "thumb_" + calendar.getTimeInMillis() + ".jpg";

        return snapshotDir + snapshotName;
    }
}
