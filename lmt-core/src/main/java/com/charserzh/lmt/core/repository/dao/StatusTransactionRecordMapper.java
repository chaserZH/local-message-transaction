package com.charserzh.lmt.core.repository.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;


public interface StatusTransactionRecordMapper extends BaseMapper<StatusTransactionRecordDO> {

    /**
     * 批量插入
     * @param paramList 列表
     */
    void batchInsert(List<StatusTransactionRecordDO> paramList);
}
