package com.yangjie.JGB28181.common.utils;

import java.io.File;

public class FileUtils {

    public static void waitFileMade(File file) {
        if (file.exists()) {
            return;
        }
        waitFileMade(file);
    }
}
