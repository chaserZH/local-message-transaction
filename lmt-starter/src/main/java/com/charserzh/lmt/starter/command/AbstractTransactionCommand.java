package com.charserzh.lmt.starter.command;

import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;



public class AbstractTransactionCommand {

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    @Resource
    private Executor executor;

    public void triggerCallback(Supplier<Void> supplier) {
        Map<String, String> map = MDC.getCopyOfContextMap();
        log.info("current thread:{} with mdc:{}", Thread.currentThread(), map);
        // 判断当前是否存在事务
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 无事务，直接提交开始
            asyncRun(supplier, map);
            return;
        }
        // 有事务，添加一个事务同步器，重写afterCommit方法，
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                // 直接提交
                asyncRun(supplier, map);
            }
        });
    }

    private void asyncRun(Supplier<Void> supplier, Map<String, String> map) {
        CompletableFuture
                .runAsync(() -> {
                    Optional.ofNullable(map).ifPresent(MDC::setContextMap);
                    supplier.get();
                }, executor)
                .exceptionally(throwable -> {
                    log.error("trigger callback error", throwable);
                    return null;
                })
                .whenComplete((unused, throwable) -> {
                    log.info("clear thread:{} mdc:{}", Thread.currentThread(), MDC.getCopyOfContextMap());
                    MDC.clear();
                });
    }
}
