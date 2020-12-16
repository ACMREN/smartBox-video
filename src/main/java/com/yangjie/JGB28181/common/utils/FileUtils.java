package com.yangjie.JGB28181.common.utils;

import java.io.File;

public class FileUtils {

    public static void waitFileMade(File file) throws InterruptedException {
        // 先休眠3s
        Thread.sleep(3000);
        if (file.exists()) {
            return;
        }
        waitFileMade(file);
    }
}
