package com.shopverse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Ch11-01: Async config — virtual thread executor (Java 21) for @Async methods.
 * Ch03-05: Configures Spring's async task execution.
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        // Ch11-03: Java 21 virtual threads — no pool sizing needed
        return command -> Thread.ofVirtual().name("async-", 0).start(command);
    }
}
