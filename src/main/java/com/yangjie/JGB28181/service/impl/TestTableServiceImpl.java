package com.yangjie.JGB28181.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.TestTable;
import com.yangjie.JGB28181.mapper.TestTableMapper;
import com.yangjie.JGB28181.service.TestTableService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author karl
 * @since 2020-10-26
 */
@Service
public class TestTableServiceImpl extends ServiceImpl<TestTableMapper, TestTable> implements TestTableService {

    @Override
    public TestTable testGetData() {
        TestTable data = super.getBaseMapper().selectById(1);

        return data;
    }
}
