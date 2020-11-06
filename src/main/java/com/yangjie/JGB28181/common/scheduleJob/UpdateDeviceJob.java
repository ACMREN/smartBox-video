package com.yangjie.JGB28181.common.scheduleJob;

import com.yangjie.JGB28181.bean.Device;
import com.yangjie.JGB28181.common.constants.BaseConstants;
import com.yangjie.JGB28181.common.constants.DeviceConstants;
import com.yangjie.JGB28181.common.utils.DateUtils;
import com.yangjie.JGB28181.entity.*;
import com.yangjie.JGB28181.entity.enumEntity.LinkStatusEnum;
import com.yangjie.JGB28181.entity.enumEntity.LinkTypeEnum;
import com.yangjie.JGB28181.entity.enumEntity.NetStatusEnum;
import com.yangjie.JGB28181.entity.enumEntity.NetTypeEnum;
import com.yangjie.JGB28181.entity.vo.LiveCamInfoVo;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.yangjie.JGB28181.service.IDeviceManagerService;
import com.yangjie.JGB28181.web.controller.ActionController;
import com.yangjie.JGB28181.web.controller.DeviceManagerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class UpdateDeviceJob {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IDeviceManagerService deviceManagerService;

    @Autowired
    private CameraInfoService cameraInfoService;

    /**
     * 每1分钟更新一次设备
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void updateDevice() {
        deviceManagerService.updateDevice();
    }
}
