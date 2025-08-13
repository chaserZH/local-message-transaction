package com.charserzh.lmt.starter;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;


@ComponentScan(basePackages = {"com.charserzh.lmt"})
@SpringBootApplication
public class Application {


    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        // 打印所有 Bean 名称
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("Loaded Beans:");
        for (String name : beanNames) {
            System.out.println(name);
        }

        // 阻塞主线程，防止进程退出
        Thread.currentThread().join(500000);
    }

}
