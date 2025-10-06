package com.kevindai.git.helper.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MrConfig {

    @Bean(name = "mrAnalysisExecutor")
    public Executor mrAnalysisExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(5);
        exec.setMaxPoolSize(5);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("mr-analysis-");
        exec.setAllowCoreThreadTimeOut(false);
        exec.initialize();
        return exec;
    }
}