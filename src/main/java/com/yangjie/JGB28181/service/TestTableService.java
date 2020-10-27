package com.yangjie.JGB28181.service;

import com.yangjie.JGB28181.common.result.GBResult;
import com.yangjie.JGB28181.entity.TestTable;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author karl
 * @since 2020-10-26
 */
public interface TestTableService extends IService<TestTable> {

    public TestTable testGetData();

}
