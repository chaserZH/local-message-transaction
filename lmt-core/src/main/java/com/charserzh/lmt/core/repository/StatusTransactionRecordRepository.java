package com.charserzh.lmt.core.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charserzh.lmt.core.config.job.LmtTaskParam;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;

import java.util.List;

public interface StatusTransactionRecordRepository {

    /**
     * 保存
     * @param paramStatusTransactionRecordEntity 实体
     * @return 实体
     */
    StatusTransactionRecordEntity save(StatusTransactionRecordEntity paramStatusTransactionRecordEntity);


    void update(StatusTransactionRecordEntity paramStatusTransactionRecordEntity);

    Page<StatusTransactionRecordEntity> findByParam(LmtTaskParam paramLmtTaskParam);

    void batchSave(List<StatusTransactionRecordEntity> paramList);

    void batchDelete(List<Long> paramList);

    void delete(Long paramLong);
}
