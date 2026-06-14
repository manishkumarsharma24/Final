package com.shopverse.infrastructure.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * Ch06-03: Cassandra keyspace bootstrap + session provider.
 *
 * This entire configuration is conditional on cassandra.enabled=true (the default).
 * Set cassandra.enabled=false in application.yml (or as an env var / system property)
 * to skip all Cassandra beans and use the NoOpOrderActivityAdapter fallback instead.
 *
 * Spring Boot's CassandraAutoConfiguration, CassandraDataAutoConfiguration, and
 * CassandraRepositoriesAutoConfiguration are excluded in @SpringBootApplication, so
 * this class is the single source of truth for all Cassandra bean wiring.
 *
 * On startup:
 *   1. A temporary no-keyspace session bootstraps the keyspace + table (idempotent).
 *   2. A real keyspace-scoped session is returned for use by CassandraTemplate
 *      and Spring Data repositories.
 */
@Configuration
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "true", matchIfMissing = true)
@EnableCassandraRepositories(basePackages = "com.shopverse.infrastructure.cassandra")
public class CassandraConfig {

    private static final Logger log = LoggerFactory.getLogger(CassandraConfig.class);

    @Value("${spring.cassandra.contact-points:localhost}")
    private String contactPoints;

    @Value("${spring.cassandra.port:9042}")
    private int port;

    @Value("${spring.cassandra.local-datacenter:datacenter1}")
    private String localDatacenter;

    @Value("${spring.cassandra.keyspace-name:shopverse}")
    private String keyspace;

    /**
     * Builds a CqlSession, bootstrapping the keyspace and table first.
     * No CqlSessionBuilder parameter — we build the session entirely here
     * because CassandraAutoConfiguration is excluded from @SpringBootApplication.
     */
    @Bean(destroyMethod = "close")
    public CqlSession cassandraSession() {
        DriverConfigLoader cfg = DriverConfigLoader.programmaticBuilder()
                .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
                .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofSeconds(10))
                .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(10))
                .build();

        // Step 1: bootstrap — create keyspace + table using a no-keyspace session
        try (CqlSession bootstrap = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(contactPoints, port))
                .withLocalDatacenter(localDatacenter)
                .withConfigLoader(cfg)
                .build()) {

            bootstrap.execute(
                "CREATE KEYSPACE IF NOT EXISTS " + keyspace +
                " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}" +
                " AND durable_writes = true"
            );
            bootstrap.execute(
                "CREATE TABLE IF NOT EXISTS " + keyspace + ".order_activity (" +
                "  customer_id  bigint," +
                "  event_time   timestamp," +
                "  event_id     uuid," +
                "  order_id     bigint," +
                "  event_type   text," +
                "  details      text," +
                "  PRIMARY KEY (customer_id, event_time, event_id)" +
                ") WITH CLUSTERING ORDER BY (event_time DESC, event_id ASC)"
            );
            log.info("Cassandra keyspace '{}' and order_activity table ensured.", keyspace);

        } catch (Exception ex) {
            // Bootstrap failed — Cassandra may be starting up or temporarily unreachable.
            // Log and continue; the real session below will either succeed or propagate
            // a clean startup failure (instead of a cryptic null-pointer later).
            log.warn("Cassandra keyspace bootstrap failed: {}. Proceeding to build session anyway.", ex.getMessage());
        }

        // Step 2: build the real keyspace-scoped session
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(contactPoints, port))
                .withLocalDatacenter(localDatacenter)
                .withKeyspace(keyspace)
                .withConfigLoader(cfg)
                .build();
    }

    /**
     * CassandraTemplate used by Spring Data repositories and direct CQL execution.
     * Replaces the one normally provided by CassandraDataAutoConfiguration.
     */
    @Bean
    public CassandraTemplate cassandraTemplate(CqlSession session) {
        return new CassandraTemplate(session);
    }
}
