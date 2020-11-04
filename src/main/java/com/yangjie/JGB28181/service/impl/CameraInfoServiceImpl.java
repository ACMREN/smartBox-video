package com.yangjie.JGB28181.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.entity.CameraInfo;
import com.yangjie.JGB28181.mapper.CameraInfoMapper;
import com.yangjie.JGB28181.service.CameraInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author karl
 * @since 2020-10-28
 */
@Service
public class CameraInfoServiceImpl extends ServiceImpl<CameraInfoMapper, CameraInfo> implements CameraInfoService {

    @Override
    public List<CameraInfo> getAllData() {
        List<CameraInfo> cameraInfos = super.getBaseMapper().selectList(null);
        return cameraInfos;
    }

    @Override
    public CameraInfo getDataByDeviceBaseId(Integer deviceBaseId) {
        CameraInfo data = super.getBaseMapper().selectOne(new QueryWrapper<CameraInfo>().eq("device_base_id", deviceBaseId));
        return data;
    }
}
