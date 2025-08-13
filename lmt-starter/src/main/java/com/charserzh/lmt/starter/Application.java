package com.charserzh.lmt.starter;


import com.charserzh.lmt.core.config.job.LmtTask;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class Application {


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
