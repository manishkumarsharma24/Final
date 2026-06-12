package com.shopverse.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Explicitly registers the JPA PlatformTransactionManager as the @Primary
 * transaction manager so that plain @Transactional annotations resolve to it
 * when multiple TransactionManager beans are present (e.g. the reactive
 * TransactionManager auto-configured by spring-boot-starter-data-neo4j).
 */
@Configuration
public class TransactionManagerConfig {

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
