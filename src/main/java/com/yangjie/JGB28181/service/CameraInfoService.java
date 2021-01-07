package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yangjie.JGB28181.entity.bo.CameraPojo;
import com.yangjie.JGB28181.entity.condition.GBDevicePlayCondition;

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

    CameraPojo openStream(CameraPojo pojo);

    GBResult gbDevicePlay(GBDevicePlayCondition gbDevicePlayCondition) throws Exception;

}
