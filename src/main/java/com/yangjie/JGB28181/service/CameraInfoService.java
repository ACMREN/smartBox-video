package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.entity.CameraInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author karl
 * @since 2020-10-28
 */
public interface CameraInfoService extends IService<CameraInfo> {

    List<CameraInfo> getAllData();

    CameraInfo getDataByDeviceBaseId(Integer deviceBaseId);

}
