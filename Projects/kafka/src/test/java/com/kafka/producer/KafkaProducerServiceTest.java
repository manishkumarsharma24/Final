package com.kafka.producer;

import com.kafka.config.producer.KafkaProducerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"my-topic"})
public class KafkaProducerServiceTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void testSendMessage() throws InterruptedException {
        // Set up a temporary consumer to catch the producer's message
//        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
//        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
//
//        ContainerProperties containerProperties = new ContainerProperties("my-topic");
//        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(cf, containerProperties);
//
//        BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();
//        container.setupMessageListener((MessageListener<String, String>) records::add);
//        container.start();
//
//        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
//
//        // Act: Trigger the producer method
//        String expectedMessage = "Test Message for Kafka";
//        kafkaProducerService.sendMessage(expectedMessage);
//
//        // Assert: Read from the queue and verify the message matching
//        ConsumerRecord<String, String> receivedRecord = records.poll(5, TimeUnit.SECONDS);

//        assertThat(receivedRecord).isNotNull();
//        assertThat(receivedRecord.value()).isEqualTo(expectedMessage);

        // Clean up
       // container.stop();
    }
}

