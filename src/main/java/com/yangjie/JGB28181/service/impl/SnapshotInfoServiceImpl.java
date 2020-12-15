package com.yangjie.JGB28181.service.impl;

import com.yangjie.JGB28181.entity.FileCountInfo;
import com.yangjie.JGB28181.entity.SnapshotInfo;
import com.yangjie.JGB28181.mapper.SnapshotInfoMapper;
import com.yangjie.JGB28181.service.SnapshotInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 截图文件信息表 服务实现类
 * </p>
 *
 * @author karl
 * @since 2020-12-14
 */
@Service
public class SnapshotInfoServiceImpl extends ServiceImpl<SnapshotInfoMapper, SnapshotInfo> implements SnapshotInfoService {

    @Autowired
    private SnapshotInfoMapper snapshotInfoMapper;

    @Override
    public List<FileCountInfo> countDataByDate(Integer deviceBaseId, String startTime, String endTime) {
        return snapshotInfoMapper.countDataByDate(deviceBaseId, startTime, endTime);
    }
}
