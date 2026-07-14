package com.kafka.config.consumer;


import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class KafkaConsumerService {

    private List<String> messages = Collections.synchronizedList(new ArrayList<>());

    @KafkaListener(topics = "my-topic", groupId = "my-group-id")
    public void consumeFullRecord(ConsumerRecord<String, String> record) {

        // 1. How to get the actual message value:
        String messageValue = record.value();

        // 2. How to get other useful metadata from the broker:
        String messageKey = record.key();
        int partition = record.partition();
        long offset = record.offset();
        long timestamp = record.timestamp();

        System.out.println("--- New Message Received ---");
        System.out.println("Value: " + messageValue);
        System.out.println("Key: " + messageKey);
        System.out.println("Partition: " + partition);
        System.out.println("Offset: " + offset);
        messages.add(messageValue);
    }

    public List<String> readMessages() {
        return messages;
    }

    public void clearMessages() {
        messages.clear();
    }

}
