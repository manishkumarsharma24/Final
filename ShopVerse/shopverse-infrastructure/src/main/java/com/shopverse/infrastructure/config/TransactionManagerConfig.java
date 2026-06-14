package com.shopverse.infrastructure.config;

import jakarta.persistence.EntityManagerFactory;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Explicitly registers both transaction managers so that:
 *   - plain @Transactional resolves to JPA (the @Primary one)
 *   - @Transactional("neo4jTransactionManager") resolves to Neo4j
 *
 * Without the explicit Neo4jTransactionManager bean, Spring Data Neo4j's
 * auto-configuration is suppressed by @ConditionalOnMissingBean (because the
 * JPA bean above already claims the "transactionManager" name), leaving Neo4j
 * with no transaction manager and causing NullPointerException on
 * TransactionTemplate inside the repository proxy.
 */
@Configuration
public class TransactionManagerConfig {

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "neo4jTransactionManager")
    public PlatformTransactionManager neo4jTransactionManager(
            Driver driver,
            DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}
