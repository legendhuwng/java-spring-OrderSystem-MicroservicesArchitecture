// PaymentEventConsumer.java
package com.example.payment_service.kafka;

import com.example.payment_service.dto.InventoryReservedEvent;
import com.example.payment_service.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topics.inventory-reserved}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onInventoryReserved(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        log.info("Received inventory-reserved: orderId={}", orderId);
        try {
            InventoryReservedEvent event = objectMapper.readValue(payload, InventoryReservedEvent.class);
            paymentService.processPayment(orderId, event);
        } catch (Exception e) {
            log.error("Failed to process payment: orderId={}", orderId, e);
        }
    }
}