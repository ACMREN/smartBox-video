package com.yangjie.JGB28181.common.utils;

import com.yangjie.JGB28181.web.controller.ActionController;

import java.io.File;

public class FileUtils {

    /**
     * 等待文件生成
     * @param file
     * @throws InterruptedException
     */
    public static void waitFileMade(File file) throws InterruptedException {
        // 先休眠3s
        Thread.sleep(3000);
        if (file.exists()) {
            return;
        }
        waitFileMade(file);
    }

    /**
     * 等待截图完成
     * @param deviceBaseId
     * @param isSnapshot
     * @throws InterruptedException
     */
    public static void waitSnapshot(Integer deviceBaseId, Boolean isSnapshot) throws InterruptedException {
        Thread.sleep(1000);
        if (!isSnapshot) {
            return;
        }
        isSnapshot = CacheUtil.deviceSnapshotMap.get(deviceBaseId);
        waitSnapshot(deviceBaseId, isSnapshot);
    }
}
