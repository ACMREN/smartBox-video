package com.yangjie.JGB28181.common.scheduleJob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GCJob {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Scheduled(cron = "0 0/15 * * * ?")
    public void gcTest() {
        logger.info("==================start gc=========================");
        System.gc();
        logger.info("==================finish gc=========================");
    }

}
