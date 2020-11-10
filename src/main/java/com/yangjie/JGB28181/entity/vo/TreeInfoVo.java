package com.yangjie.JGB28181.entity.vo;

import com.yangjie.JGB28181.entity.TreeInfo;
import com.yangjie.JGB28181.entity.enumEntity.TreeTypeEnum;
import lombok.Data;

@Data
public class TreeInfoVo {
    private Integer id;
    private String treeInfo;
    private String pollingList;
    private String treeType;

    public TreeInfoVo(TreeInfo treeInfo) {
        this.id = treeInfo.getId();
        this.treeInfo = treeInfo.getTreeInfo();
        this.pollingList = treeInfo.getPollingList();
        this.treeType = TreeTypeEnum.getDataByCode(treeInfo.getTreeType()).getName();
    }
}
