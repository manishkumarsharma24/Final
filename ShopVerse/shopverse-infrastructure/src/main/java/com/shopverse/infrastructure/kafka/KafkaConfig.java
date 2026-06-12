package com.shopverse.infrastructure.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka infrastructure configuration.
 *
 * @RetryableTopic activation:
 *   In Spring Boot 3.2.x / Spring Kafka 3.1.x, @RetryableTopic is auto-activated by
 *   Spring Boot's KafkaAnnotationDrivenConfiguration. No @EnableRetryTopic annotation
 *   is needed (that annotation does not exist in this version of spring-kafka).
 *
 * Explicit kafkaListenerContainerFactory:
 *   Spring Boot auto-configures one, but defining it here gives full control over:
 *     - Concurrency (threads per listener, matched to topic partition count)
 *     - Error handling strategy (DefaultErrorHandler with fixed back-off)
 *     - DLT routing (DeadLetterPublishingRecoverer sends poison pills to <topic>.DLT)
 *     - Non-retryable exceptions (DeserializationException goes straight to DLT)
 *
 * Deserialization:
 *   JsonSerializer (producer) automatically adds a __TypeId__ header containing the
 *   fully-qualified class name. JsonDeserializer (consumer) reads this header to
 *   instantiate the correct sealed-interface subtype (e.g. OrderEvent$OrderPlaced).
 *   Trusted packages: spring.json.trusted.packages=com.shopverse.* (application.yml)
 *
 * @RetryableTopic integration:
 *   @RetryableTopic on OrderKafkaConsumer.consume() creates retry topics and routes
 *   exhausted messages to the DLT. DeadLetterPublishingRecoverer here handles errors
 *   that escape @RetryableTopic (e.g. deserialization failures that can't be retried).
 */
@Configuration
public class KafkaConfig {

    /**
     * Explicit kafkaListenerContainerFactory bean.
     *
     * Concurrency=3: one thread per partition on 3-partition topics (orders, products, inventory).
     * Analytics topic has 6 partitions — scale that listener's concurrency separately if needed.
     */
    @Bean
    @SuppressWarnings("unchecked")
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<?, ?> kafkaConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory((ConsumerFactory<String, Object>) kafkaConsumerFactory);
        factory.setConcurrency(3);

        // Route failed messages (after all retries) to <originalTopic>.DLT
        // Partition is preserved so DLT messages can be correlated to source partition.
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // 2 immediate retries, 1 second apart — coarse-grained safety net.
        // Fine-grained retry logic (3 attempts + exponential backoff) is on
        // @RetryableTopic in OrderKafkaConsumer.
        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 2L));

        // DeserializationException means the payload is corrupt — retrying won't help.
        // Send straight to DLT rather than burning retry attempts.
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
