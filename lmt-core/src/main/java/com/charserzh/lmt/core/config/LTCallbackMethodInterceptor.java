package com.charserzh.lmt.core.config;

import com.charserzh.lmt.core.eums.TransactionExecStatusEnum;
import com.charserzh.lmt.core.model.CallbackResultValue;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;
import com.charserzh.lmt.core.repository.StatusTransactionRecordRepository;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AOP 拦截器
 * 1. 拦截本地消息事务的回调方法
 * 2. 回调方法执行完成后，更新事务状态
 */
public class LTCallbackMethodInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LTCallbackMethodInterceptor.class);

    private final StatusTransactionRecordRepository repository;

    public LTCallbackMethodInterceptor(StatusTransactionRecordRepository repository) {
        this.repository = repository;
    }


    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        log.info("local message transaction callback: {}, method: {}, args: {}", invocation.getThis(), invocation.getMethod().getName(), invocation.getArguments());

        if ("callback".equals(invocation.getMethod().getName())) {
            StatusTransactionRecordEntity entity = (StatusTransactionRecordEntity) invocation.getArguments()[0];
            try {
                if (TransactionExecStatusEnum.codeOf(entity.getExecStatus()) == TransactionExecStatusEnum.SUCCESS) {
                    log.warn("transaction record already success, ignore: {}", entity);
                    return null;
                }
                CallbackResultValue result = (CallbackResultValue) invocation.proceed();
                if (result != null && result.isResult()) {
                    entity.setExecStatus(1);
                    entity.setIsDelete(1);
                } else if (result != null) {
                    entity.setExecStatus(2);
                    entity.setErrorMessage(result.getMessage());
                }
                entity.setExecTimes((entity.getExecTimes() == null ? 0 : entity.getExecTimes()) + 1);
                repository.update(entity);
            } catch (Exception e) {
                log.error("local message transaction callback error", e);
                entity.setErrorMessage(e.getMessage());
                entity.setExecStatus(0);
                entity.setExecTimes((entity.getExecTimes() == null ? 0 : entity.getExecTimes()) + 1);
                repository.update(entity);
            }
        }
        return invocation.proceed();
    }


}
