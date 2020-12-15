package com.yangjie.JGB28181.service.impl;

import com.yangjie.JGB28181.entity.FileCountInfo;
import com.yangjie.JGB28181.entity.RecordVideoInfo;
import com.yangjie.JGB28181.mapper.RecordVideoInfoMapper;
import com.yangjie.JGB28181.service.RecordVideoInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 录像文件信息表 服务实现类
 * </p>
 *
 * @author karl
 * @since 2020-12-11
 */
@Service
public class RecordVideoInfoServiceImpl extends ServiceImpl<RecordVideoInfoMapper, RecordVideoInfo> implements RecordVideoInfoService {

    @Autowired
    private RecordVideoInfoMapper recordVideoInfoMapper;

    @Override
    public List<FileCountInfo> countDataByDate(Integer deviceBaseId, String startTime, String endTime) {
        return recordVideoInfoMapper.countDataByDate(deviceBaseId, startTime, endTime);
    }
}
