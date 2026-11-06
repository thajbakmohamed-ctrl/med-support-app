package com.app.med_support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ReminderExecutorConfig {


    @Bean(destroyMethod = "shutdown")
    public ExecutorService reminderExecutor() {

        return Executors.newFixedThreadPool(3);
    }
}