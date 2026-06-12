package com.shopverse.infrastructure.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * Ch06-03: Cassandra keyspace bootstrap + session provider.
 *
 * Problem: Spring Boot's CassandraAutoConfiguration creates CqlSession with
 * keyspace-name set, but the keyspace may not exist yet — causing
 * InvalidKeyspaceException at startup.
 *
 * Solution: provide our own CqlSession @Bean (suppresses the auto-configured one
 * via @ConditionalOnMissingBean). Before building the real session we open a
 * temporary no-keyspace session (with a 30-second timeout to handle a slow/warming
 * Cassandra container), CREATE the keyspace idempotently, then hand control back to
 * Spring Boot's CqlSessionBuilder which already has all driver settings wired in
 * from application.yml.
 *
 * Fallback: if Cassandra is unreachable, the app starts without Cassandra features.
 * spring.cassandra.schema-action must be NONE — Spring Data must not attempt its
 * own DDL on a no-keyspace session.
 */
@Configuration
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
     * Replaces CassandraAutoConfiguration#cassandraSession (@ConditionalOnMissingBean).
     * Bootstraps keyspace before the keyspace-scoped session is opened.
     */
    @Bean(destroyMethod = "close")
    @Primary
    public CqlSession cassandraSession(CqlSessionBuilder cqlSessionBuilder) {
        // Bootstrap: create keyspace using a temporary no-keyspace session.
        // 30-second request timeout accommodates a slow-starting Cassandra container.
        DriverConfigLoader bootstrapConfig = DriverConfigLoader.programmaticBuilder()
                .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
                .withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofSeconds(10))
                .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(10))
                .build();

        try (CqlSession bootstrap = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(contactPoints, port))
                .withLocalDatacenter(localDatacenter)
                .withConfigLoader(bootstrapConfig)
                .build()) {

            bootstrap.execute(
                "CREATE KEYSPACE IF NOT EXISTS " + keyspace +
                " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}" +
                " AND durable_writes = true"
            );
            // Qualify table name with keyspace so it works from a no-keyspace session
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
            log.warn("Cassandra bootstrap failed ({}). App will start without Cassandra features.", ex.getMessage());
            // Return a no-keyspace session so the context finishes loading.
            // schema-action=NONE prevents Spring Data from issuing any DDL here.
            try {
                return CqlSession.builder()
                        .addContactPoint(new InetSocketAddress(contactPoints, port))
                        .withLocalDatacenter(localDatacenter)
                        .withConfigLoader(bootstrapConfig)
                        .build();
            } catch (Exception fallbackEx) {
                log.warn("Cassandra fallback session also failed: {}", fallbackEx.getMessage());
                throw new RuntimeException("Cassandra is unreachable and could not build a fallback session", fallbackEx);
            }
        }

        // Build the real keyspace-scoped session via Spring Boot's builder
        return cqlSessionBuilder.build();
    }
}
