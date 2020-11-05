package com.yangjie.JGB28181.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yangjie.JGB28181.entity.TreeInfo;
import com.yangjie.JGB28181.mapper.TreeInfoMapper;
import com.yangjie.JGB28181.service.TreeInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 树状图表 服务实现类
 * </p>
 *
 * @author karl
 * @since 2020-11-04
 */
@Service
public class TreeInfoServiceImpl extends ServiceImpl<TreeInfoMapper, TreeInfo> implements TreeInfoService {

    @Override
    public TreeInfo getDataByUserAndType(Integer userId, Integer type) {
        TreeInfo data = super.getBaseMapper().selectOne(new QueryWrapper<TreeInfo>().eq("user_id", userId).eq("tree_type", type));
        return data;
    }
}
