package com.kafka.controller;

import com.kafka.config.consumer.KafkaConsumerService;
import com.kafka.config.producer.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private final KafkaProducerService kafkaProducerService;
    private final KafkaConsumerService kafkaConsumerService;

    public KafkaController(KafkaProducerService kafkaProducerService, KafkaConsumerService kafkaConsumerService) {
        this.kafkaProducerService = kafkaProducerService;
        this.kafkaConsumerService = kafkaConsumerService;
    }

    @PostMapping("/publish")
    ResponseEntity<String>  publishMessage(@RequestParam("message")  String message) {
        kafkaProducerService.sendMessage(message);
        return ResponseEntity.ok("Message published");
    }

    @GetMapping("/messages")
    ResponseEntity<List<String>> getMessages() {
        List<String> messages = kafkaConsumerService.readMessages();
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/clearMessages")
    ResponseEntity<String> clearMessages() {
        kafkaConsumerService.clearMessages();
        ResponseEntity<String> response = ResponseEntity.ok("Messages cleared");
        return response;
    }

}
