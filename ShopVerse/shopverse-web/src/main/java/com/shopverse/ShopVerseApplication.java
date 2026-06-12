package com.shopverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Ch03-01: @SpringBootApplication — auto-config, component scan, config.
 * Ch03-05: @EnableAsync — virtual-thread executor (Java 21).
 * Ch11-05: @EnableCaching — Redis cache abstraction.
 * Ch09-04: @EnableRetry — Spring Retry for transient failures.
 *
 * Note: @RetryableTopic is auto-activated by Spring Boot's KafkaAnnotationDrivenConfiguration
 * in Spring Boot 3.2.x / Spring Kafka 3.1.x. No explicit @EnableRetryTopic needed.
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableRetry
public class ShopVerseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopVerseApplication.class, args);
    }
}
