package com.pm.greatadamu.customerservice.kafka;

import com.pm.greatadamu.customerservice.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserRegisteredEventListener {
    private final CustomerService customerService ;

    @KafkaListener(topics = "user-registrations",groupId = "customer-service")
    public void handleUserRegistrationEvent(UserRegisteredEvent userRegisteredEvent) {
        log.info("Received user registration event {}", userRegisteredEvent);

        // Hand off to business logic
        customerService.createCustomerFromEvent(userRegisteredEvent);

    }
}
