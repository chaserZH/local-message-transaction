package com.charserzh.lmt.starter.command;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.charserzh.lmt.core.callback.LTCallback;
import com.charserzh.lmt.core.model.StatusTransactionRecordEntity;
import com.charserzh.lmt.core.repository.StatusTransactionRecordRepository;
import com.charserzh.lmt.starter.test.TestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.UUID;


@Component
public class TransactionTestCommand extends AbstractTransactionCommand {
    private final Logger log = LoggerFactory.getLogger(TransactionTestCommand.class);

    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private StatusTransactionRecordRepository statusTransactionRecordRepository;

    @Resource
    private LTCallback testCallback;



    public void testLmt(){
        log.info("test enter");
        transactionTemplate.execute(status -> {
            // 修改记录
            updateOrderStatus();
            // 提交本地事务消息
            StatusTransactionRecordEntity save = statusTransactionRecordRepository.save(new StatusTransactionRecordEntity( // insert slave  biz record
                    "test_lmt",
                    "test_lmt",
                    UUID.randomUUID().toString(),
                    JSONUtil.toJsonStr(new TestEntity(1L, "test")))
            );
//
            triggerCallback(() -> {
                testCallback.callback(save);
                return null;
            });
            return null;
        });
    }

    /**
     * 更新订单状态
     */
    private void updateOrderStatus() {
        log.info("update order status");
    }
}
