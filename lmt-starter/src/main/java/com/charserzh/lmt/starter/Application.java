package com.charserzh.lmt.starter;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = "com.charserzh.lmt")
public class Application {


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
