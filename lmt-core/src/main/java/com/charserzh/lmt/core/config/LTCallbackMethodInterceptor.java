package com.charserzh.lmt.core.config;

import cn.hutool.json.JSONUtil;
import com.charserzh.lmt.core.eums.TransactionExecStatusEnum;
import com.charserzh.lmt.core.model.CallbackResultValue;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;
import com.charserzh.lmt.core.repository.StatusTransactionRecordRepository;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

public class LTCallbackMethodInterceptor implements MethodInterceptor, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(LTCallbackMethodInterceptor.class);

    private ApplicationContext applicationContext;

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
        log.info("local message transaction callback:{},{},{}", new Object[] { invocation.getThis(), invocation.getMethod().getName(), invocation.getArguments() });
        Method method = invocation.getMethod();
        Object[] args = invocation.getArguments();
        if (method.getName().equals("callback")) {
            Object result;
            StatusTransactionRecordEntity entity = (StatusTransactionRecordEntity)args[0];
            try{
                if (TransactionExecStatusEnum.codeOf(entity.getExecStatus()) == TransactionExecStatusEnum.SUCCESS) {
                    log.warn("transaction record already been success, ignore,{}", JSONUtil.toJsonStr(entity));
                    return null;
                }
                result = invocation.proceed();
                if (Objects.isNull(result)){
                    return result;
                }
                CallbackResultValue callbackResultValue = (CallbackResultValue)result;
                if (callbackResultValue.isResult()) {
                    log.info("testing exec update success");
                    entity.setExecStatus(1);
                    entity.setIsDelete(1);
                } else {
                    entity.setExecStatus(2);
                    entity.setErrorMessage(callbackResultValue.getMessage());
                }
                entity.setExecTimes(Optional.ofNullable(entity.getExecTimes()).orElse(0) + 1);
                updateEntity(entity);
            }catch (Exception e){
                log.error("local message transaction callback error", e);
                entity.setErrorMessage(e.getMessage());
                entity.setExecStatus(0);
                entity.setExecTimes(Optional.ofNullable(entity.getExecTimes()).orElse(0) + 1);
                updateEntity(entity);
            }
        }
        return invocation.proceed();
    }

    private void updateEntity(StatusTransactionRecordEntity entity) {
        StatusTransactionRecordRepository statusTransactionRecordRepository = (StatusTransactionRecordRepository)this.applicationContext.getBean(StatusTransactionRecordRepository.class);
        statusTransactionRecordRepository.update(entity);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
