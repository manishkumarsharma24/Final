package com.shopverse.infrastructure.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka infrastructure configuration — fully explicit, no auto-config reliance.
 *
 * WHY EXPLICIT?
 *   In a Maven multi-module build, Spring Boot's KafkaAutoConfiguration picks up
 *   spring.kafka.* properties from application.yml, but the JsonSerializer /
 *   JsonDeserializer settings (spring.json.*) are sometimes not propagated to the
 *   factory before it initialises. Defining ProducerFactory and ConsumerFactory
 *   explicitly guarantees the correct serializers and type-header behaviour regardless
 *   of module loading order.
 *
 * PRODUCER (JsonSerializer):
 *   - Serialises domain event POJOs (OrderEvent, AnalyticsEvent, …) to JSON.
 *   - ADD_TYPE_INFO_HEADERS=true → writes __TypeId__ header = fully-qualified class name
 *     (e.g. com.shopverse.domain.event.OrderEvent$OrderPlaced).
 *
 * CONSUMER (JsonDeserializer):
 *   - USE_TYPE_INFO_HEADERS=true  → reads __TypeId__ header to pick the target class.
 *   - TRUSTED_PACKAGES           → allows all com.shopverse.* classes to be instantiated.
 *   - Without these two settings the deserializer falls back to LinkedHashMap, which
 *     makes "record.value() instanceof OrderEvent event" always false.
 *
 * kafkaListenerContainerFactory:
 *   Concurrency=3: one thread per partition on 3-partition topics.
 *   DefaultErrorHandler + DeadLetterPublishingRecoverer routes failed messages to <topic>.DLT.
 *   DeserializationException is non-retryable — goes straight to DLT.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.retries:3}")
    private int retries;

    @Value("${spring.kafka.consumer.group-id:shopverse-consumer}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    // ── Producer ─────────────────────────────────────────────────────────────

    /**
     * ProducerFactory wired with JsonSerializer.
     * Key: String (orderId, sessionId, etc.)
     * Value: JSON + __TypeId__ header for sealed-interface deserialization on consumer side.
     */
    @Bean
    @Primary
    public ProducerFactory<String, Object> kafkaProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, acks);
        config.put(ProducerConfig.RETRIES_CONFIG, retries);
        // Writes __TypeId__ header = fully-qualified class name of the event
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }

    // ── Consumer ─────────────────────────────────────────────────────────────

    /**
     * ConsumerFactory wired with JsonDeserializer configured to read __TypeId__ headers.
     *
     * USE_TYPE_INFO_HEADERS=true  → reads the __TypeId__ header the producer wrote.
     * TRUSTED_PACKAGES           → allows instantiation of any com.shopverse.* class.
     *
     * Without these settings, Jackson deserialises the JSON payload into a LinkedHashMap,
     * and "record.value() instanceof OrderEvent event" always evaluates to false — the
     * consumer switch never fires.
     */
    @Bean
    @Primary
    public ConsumerFactory<String, Object> kafkaConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // Read __TypeId__ header to determine the target class (not a fixed target type)
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        // Trust all ShopVerse domain classes so they can be instantiated during deserialization
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.shopverse.*");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    // ── Listener container factory ────────────────────────────────────────────

    /**
     * Listener container factory.
     * Concurrency=3: one thread per partition on 3-partition topics (orders, products, inventory).
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> kafkaConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(kafkaConsumerFactory);
        factory.setConcurrency(3);

        // Route exhausted-retry messages to <topic>.DLT (partition-preserving)
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // 2 immediate retries at 1-second intervals (coarse-grained safety net).
        // Fine-grained retry is handled by @RetryableTopic on each consumer.
        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));

        // Corrupt payload — retrying will never fix it, send straight to DLT
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
