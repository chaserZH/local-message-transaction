package com.charserzh.lmt.starter;


import cn.hutool.core.thread.NamedThreadFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.charserzh.lmt")
public class Application {

    @Bean
    public Executor executor() {
        return new ThreadPoolExecutor(
                5,
                10,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2040, true),
                new NamedThreadFactory("fulfillment-async", true),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }


    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("Loaded Beans:");
        for (String name : beanNames) {
            if (name.contains("Handler")) System.out.println(name);
        }

        // 阻塞主线程，保证非 Web 项目持续运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

}
