package com.yangjie.JGB28181.entity;

import lombok.Data;

import java.util.List;

@Data
public class PageListVo<T> {
    private List<T> dataList;
    // 页码
    private Integer pageNo;
    // 每页数量
    private Integer pageSize;
    // 数据总数
    private Integer total;
    // 分页数
    private Integer pageNum;

    public PageListVo(List<T> dataList, Integer pageNo, Integer pageSize, Integer total) {
        this.dataList = dataList;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        if (null != pageSize && null != total) {
            if (pageSize.intValue() != 0) {
                this.pageNum = total / pageSize;
            }
        }
    }

    public PageListVo() {

    }
}
