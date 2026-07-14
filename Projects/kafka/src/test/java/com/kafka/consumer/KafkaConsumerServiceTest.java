package com.kafka.consumer;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
//import org.springframework.boot.test.mock.SpyBean;

import static org.mockito.Mockito.timeout;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"my-topic"})
class KafkaConsumerServiceTest {
//
//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    // Use SpyBean to wrap the real bean and track method invocations
//    @SpyBean
//    private KafkaConsumerService consumerService;
//
//    @Test
//    void testConsumeMessage() {
//        String message = "Hello from Embedded Kafka!";
//
//        // Act: Publish a raw message directly to the topic
//        kafkaTemplate.send("my-topic", message);
//
//        // Assert: Verify that the consume method was invoked within a 5-second timeout window
//        Mockito.verify(consumerService, timeout(5000).times(1)).consume(message);
//    }
}
