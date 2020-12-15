package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.entity.FileCountInfo;
import com.yangjie.JGB28181.entity.RecordVideoInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 录像文件信息表 服务类
 * </p>
 *
 * @author karl
 * @since 2020-12-11
 */
public interface RecordVideoInfoService extends IService<RecordVideoInfo> {

    List<FileCountInfo> countDataByDate(Integer deviceBaseId, String startTime, String endTime);

}
