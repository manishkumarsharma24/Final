package com.shopverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Ch03-01: @SpringBootApplication — auto-config, component scan, config.
 * Ch03-05: @EnableAsync — virtual-thread executor (Java 21).
 * Ch11-05: @EnableCaching — Redis cache abstraction.
 * Ch09-04: @EnableRetry — Spring Retry for transient failures.
 *
 * Cassandra auto-configs are excluded here because CassandraConfig handles
 * session creation manually (with keyspace bootstrap + optional disable support).
 * When cassandra.enabled=false, none of the Cassandra beans load.
 */
@SpringBootApplication(exclude = {
    CassandraAutoConfiguration.class,
    CassandraDataAutoConfiguration.class,
    CassandraRepositoriesAutoConfiguration.class
})
@EnableAsync
@EnableCaching
@EnableRetry
public class ShopVerseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShopVerseApplication.class, args);
    }
}
