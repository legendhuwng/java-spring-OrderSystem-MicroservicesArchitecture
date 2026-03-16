// ShippingEventConsumer.java
package com.example.shipping_service.kafka;

import com.example.shipping_service.dto.PaymentSuccessEvent;
import com.example.shipping_service.service.ShippingService;
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
public class ShippingEventConsumer {

    private final ShippingService shippingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topics.payment-success}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onPaymentSuccess(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        log.info("Received payment-success: orderId={}", orderId);
        try {
            PaymentSuccessEvent event = objectMapper.readValue(payload, PaymentSuccessEvent.class);
            shippingService.createShipment(orderId, event);
        } catch (Exception e) {
            log.error("Failed to create shipment: orderId={}", orderId, e);
        }
    }
}