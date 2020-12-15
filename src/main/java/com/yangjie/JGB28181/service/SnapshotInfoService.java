package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.entity.FileCountInfo;
import com.yangjie.JGB28181.entity.SnapshotInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 截图文件信息表 服务类
 * </p>
 *
 * @author karl
 * @since 2020-12-14
 */
public interface SnapshotInfoService extends IService<SnapshotInfo> {

    List<FileCountInfo> countDataByDate(Integer deviceBaseId, String startTime, String endTime);

}
