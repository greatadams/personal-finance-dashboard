package com.pm.greatadamu.authservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventProducer {
    final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    private static final String TOPIC = "user-registrations";

    public void  sendUserRegisteredEvent(UserRegisteredEvent userRegisteredEvent) {
        String key= userRegisteredEvent.getEmail();
        log.info("Sending user registered event to kafka {}", userRegisteredEvent);
        kafkaTemplate.send(TOPIC, key ,userRegisteredEvent);
    }
}
