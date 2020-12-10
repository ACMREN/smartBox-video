package com.yangjie.JGB28181.common.scheduleJob;

import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

@Component
public class DeleteExpiredRecorderFile {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 每天两点半执行删除过期文件
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void deleteExpiredRecorderFile() {
        logger.info("==================开始删除过期录像文件=========================");
        String recordStTime = DeviceManagerController.cameraConfigBo.getRecordStTime();
        Long expiredTime = Long.valueOf(recordStTime);
        String recordDir = DeviceManagerController.cameraConfigBo.getRecordDir();
        File baseFilePath = new File(recordDir);
        File[] allStreamDocuments = baseFilePath.listFiles();
        for (File streamDocument : allStreamDocuments) {
            File[] allDateDocuments = streamDocument.listFiles();
            for (File dateDocument : allDateDocuments) {
                File[] allRecordFiles = dateDocument.listFiles();
                for (File recordFile : allRecordFiles) {
                    // 获取最后的记录日期
                    Date date = new Date(recordFile.lastModified());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    int recordDay = calendar.get(Calendar.DAY_OF_YEAR);

                    // 获取今天的日期
                    calendar.setTime(new Date());
                    int today = calendar.get(Calendar.DAY_OF_YEAR);

                    // 如果大于配置文件中的日期，那么就删除文件
                    Integer offset = today - recordDay;
                    Long offsetMs = offset * BaseConstants.MS_OF_DAY;
                    if (offsetMs >= expiredTime) {
                        recordFile.delete();
                    }
                }
            }
        }
        logger.info("==================结束删除过期录像文件=========================");
    }
}
