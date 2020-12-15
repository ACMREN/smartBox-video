package com.yangjie.JGB28181.mapper;

import com.yangjie.JGB28181.entity.FileCountInfo;
import com.yangjie.JGB28181.entity.RecordVideoInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 录像文件信息表 Mapper 接口
 * </p>
 *
 * @author karl
 * @since 2020-12-11
 */
public interface RecordVideoInfoMapper extends BaseMapper<RecordVideoInfo> {

    List<FileCountInfo> countDataByDate(@Param("deviceBaseId")Integer deviceBaseId, @Param("startTime")String startTime, @Param("endTime")String endTime);

}
