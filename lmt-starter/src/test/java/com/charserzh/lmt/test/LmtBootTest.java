package com.charserzh.lmt.test;


import com.charserzh.lmt.starter.Application;
import com.charserzh.lmt.starter.command.TransactionTestCommand;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;

@ActiveProfiles("local")
@SpringBootTest(classes = Application.class)
public class LmtBootTest {

    @Resource
    private TransactionTestCommand transactionTestCommand;

    @Test
    public void testLocalMessageFlow() throws InterruptedException {
        transactionTestCommand.testLmt();

        Thread.sleep(1000000000000L);
    }
}
