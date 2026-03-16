// NotificationEventConsumer.java
package com.example.notification_service.kafka;

import com.example.notification_service.dto.NotificationEvent;
import com.example.notification_service.service.NotificationService;
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
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderCreated(@Payload String payload,
                               @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        try {
            notificationService.notifyOrderCreated(orderId);
        } catch (Exception e) {
            // Notification fail không ảnh hưởng luồng chính — chỉ log
            log.error("Failed to send order-created notification: orderId={}", orderId, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.inventory-reserved}", groupId = "${spring.kafka.consumer.group-id}")
    public void onInventoryReserved(@Payload String payload,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        try {
            notificationService.notifyInventoryReserved(orderId);
        } catch (Exception e) {
            log.error("Failed to send inventory-reserved notification: orderId={}", orderId, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.inventory-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onInventoryFailed(@Payload String payload,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            notificationService.notifyInventoryFailed(orderId, event.getReason());
        } catch (Exception e) {
            log.error("Failed to send inventory-failed notification: orderId={}", orderId, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.payment-success}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentSuccess(@Payload String payload,
                                 @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        try {
            notificationService.notifyPaymentSuccess(orderId);
        } catch (Exception e) {
            log.error("Failed to send payment-success notification: orderId={}", orderId, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.payment-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(@Payload String payload,
                                @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            notificationService.notifyPaymentFailed(orderId, event.getReason());
        } catch (Exception e) {
            log.error("Failed to send payment-failed notification: orderId={}", orderId, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.shipping-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void onShippingCreated(@Payload String payload,
                                  @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            notificationService.notifyShippingCreated(orderId, event.getTrackingNumber());
        } catch (Exception e) {
            log.error("Failed to send shipping-created notification: orderId={}", orderId, e);
        }
    }
}