package com.yangjie.JGB28181.mapper;

import com.yangjie.JGB28181.entity.FileCountInfo;
import com.yangjie.JGB28181.entity.SnapshotInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 截图文件信息表 Mapper 接口
 * </p>
 *
 * @author karl
 * @since 2020-12-14
 */
public interface SnapshotInfoMapper extends BaseMapper<SnapshotInfo> {

    List<FileCountInfo> countDataByDate(@Param("deviceBaseId")Integer deviceBaseId, @Param("startTime")String startTime, @Param("endTime")String endTime);
}
