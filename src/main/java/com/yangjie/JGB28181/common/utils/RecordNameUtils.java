package com.yangjie.JGB28181.common.utils;

import com.yangjie.JGB28181.web.controller.DeviceManagerController;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

public class RecordNameUtils {

    public static String recordVideoFileAddress(String streamName) {
        String baseRecordDir = DeviceManagerController.cameraConfigBo.getRecordDir();
        Calendar calendar = Calendar.getInstance();
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
        String fileName = "record_" + new Random().nextInt(100000);
        String recordAddress = filePath + fileName + ".flv";
        File recordFile = new File(recordAddress);
        if (recordFile.exists()) {
            getRecordFileName(filePath);
        }
        return fileName;
    }
}
