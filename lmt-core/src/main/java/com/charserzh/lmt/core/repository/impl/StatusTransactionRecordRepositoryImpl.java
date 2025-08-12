package com.charserzh.lmt.core.repository.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.charserzh.lmt.core.config.job.LmtTaskParam;
import com.charserzh.lmt.core.eums.TransactionExecStatusEnum;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;
import com.charserzh.lmt.core.repository.StatusTransactionRecordRepository;
import com.charserzh.lmt.core.repository.dao.StatusTransactionRecordDO;
import com.charserzh.lmt.core.repository.dao.StatusTransactionRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class StatusTransactionRecordRepositoryImpl implements StatusTransactionRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(StatusTransactionRecordRepositoryImpl.class);

    private final StatusTransactionRecordMapper statusTransactionRecordMapper;

    public StatusTransactionRecordRepositoryImpl(StatusTransactionRecordMapper statusTransactionRecordMapper) {
        this.statusTransactionRecordMapper = statusTransactionRecordMapper;
    }

    @Override
    public StatusTransactionRecordEntity save(StatusTransactionRecordEntity recordEntity) {
        try {
            log.info("prepare insert local message transaction record,param{}", JSONUtil.toJsonStr(recordEntity));
            StatusTransactionRecordDO recordDO = BeanUtil.copyProperties(recordEntity, StatusTransactionRecordDO.class);
            recordDO.setCreateTime(System.currentTimeMillis());
            this.statusTransactionRecordMapper.insert(recordDO);
            recordEntity.setId(recordDO.getId());
            log.info("prepare insert local message transaction record,result{}", JSONUtil.toJsonStr(recordEntity));
            return recordEntity;
        } catch (Exception e) {
            if (e instanceof org.springframework.dao.DuplicateKeyException) {
                log.warn("prepare insert local message transaction record,duplicate{}", e.getMessage());
                StatusTransactionRecordDO statusTransactionRecordDO = statusTransactionRecordMapper.selectOne(
                        Wrappers.lambdaQuery(StatusTransactionRecordDO.class)
                                .eq(StatusTransactionRecordDO::getBizSceneCode, recordEntity.getBizSceneCode())
                                .eq(StatusTransactionRecordDO::getBizId, recordEntity.getBizId())
                );
                StatusTransactionRecordEntity duplicateRecord = BeanUtil.copyProperties(statusTransactionRecordDO, StatusTransactionRecordEntity.class, new String[0]);
                log.warn("prepare insert local message transaction record,return duplicate record{}", JSONUtil.toJsonStr(duplicateRecord));
                return duplicateRecord;
            }
            log.error("prepare insert local message transaction record ,exception", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(StatusTransactionRecordEntity entity) {
        final StatusTransactionRecordDO recordDO = BeanUtil.copyProperties(entity, StatusTransactionRecordDO.class);
        try {
            this.statusTransactionRecordMapper.update(new StatusTransactionRecordDO() {

            }, Wrappers.lambdaUpdate(StatusTransactionRecordDO.class).eq(StatusTransactionRecordDO::getId, recordDO.getId()));
        } catch (Exception e) {
            log.error("update status transaction error", e);
        }
    }

    @Override
    public Page<StatusTransactionRecordEntity> findByParam(LmtTaskParam lmtTaskParam) {
        // 1. 分页参数构建（带默认值）
        IPage<StatusTransactionRecordDO> page = new Page<>(
                lmtTaskParam.getCurrent() != null ? lmtTaskParam.getCurrent() : 1L,
                lmtTaskParam.getPageSize() != null ? lmtTaskParam.getPageSize() : 10L
        );

        // 2. 动态查询条件
        LambdaQueryWrapper<StatusTransactionRecordDO> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper
                .eq(StringUtils.hasText(lmtTaskParam.getBizSceneCode()),
                        StatusTransactionRecordDO::getBizSceneCode,
                        lmtTaskParam.getBizSceneCode())
                .eq(StringUtils.hasText(lmtTaskParam.getBizId()),
                        StatusTransactionRecordDO::getBizId,
                        lmtTaskParam.getBizId())
                .in(lmtTaskParam.isOnlyFailed(),
                        StatusTransactionRecordDO::getExecStatus,
                        TransactionExecStatusEnum.WAIT.getCode(),
                        TransactionExecStatusEnum.FAILED.getCode());

        // 3. 执行查询
        IPage<StatusTransactionRecordDO> doPage = statusTransactionRecordMapper.selectPage(page, queryWrapper);

        // 4. 手动转换分页结果
        Page<StatusTransactionRecordEntity> entityPage = new Page<>();
        BeanUtil.copyProperties(doPage, entityPage, "records");

        // 5. 转换records列表
        List<StatusTransactionRecordEntity> entityRecords = doPage.getRecords().stream()
                .map(doObj -> {
                    StatusTransactionRecordEntity entity = new StatusTransactionRecordEntity();
                    BeanUtil.copyProperties(doObj, entity);
                    return entity;
                })
                .collect(Collectors.toList());

        entityPage.setRecords(entityRecords);
        return entityPage;
    }

    @Override
    public void batchSave(List<StatusTransactionRecordEntity> list) {
        List<StatusTransactionRecordDO> recordDOList = list.stream().map(d -> {
            StatusTransactionRecordDO entity = BeanUtil.copyProperties(d, StatusTransactionRecordDO.class);
            entity.setCreateTime(System.currentTimeMillis());
            entity.setUpdateTime(System.currentTimeMillis());
            return entity;
        }).collect(Collectors.toList());
        this.statusTransactionRecordMapper.batchInsert(recordDOList);
    }

    @Override
    public void batchDelete(List<Long> list) {
        this.statusTransactionRecordMapper.deleteBatchIds(list);
    }

    @Override
    public void delete(Long id) {
        this.statusTransactionRecordMapper.deleteById(id);
    }
}
