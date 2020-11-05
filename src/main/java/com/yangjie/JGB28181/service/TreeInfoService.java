package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.entity.TreeInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 树状图表 服务类
 * </p>
 *
 * @author karl
 * @since 2020-11-04
 */
public interface TreeInfoService extends IService<TreeInfo> {

    TreeInfo getDataByUserAndType(Integer userId, Integer type);

}
