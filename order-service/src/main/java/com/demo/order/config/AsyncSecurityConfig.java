package com.demo.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncSecurityConfig implements AsyncConfigurer {

    @Bean
    public Executor productExecutorDelegate() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("product-");
        ex.setCorePoolSize(16);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(0); // back-pressure: pas de file d’attente => on refuse si saturé
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(5);
        ex.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(ex);
    }

}