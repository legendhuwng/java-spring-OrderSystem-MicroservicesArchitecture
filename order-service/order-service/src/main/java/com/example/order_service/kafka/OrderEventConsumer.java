// OrderEventConsumer.java
package com.example.order_service.kafka;

import com.example.order_service.entity.OrderStatus;
import com.example.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${kafka.topics.inventory-reserved}", groupId = "${spring.kafka.consumer.group-id}")
    public void onInventoryReserved(@Payload String payload,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        log.info("Inventory reserved for orderId={}", orderId);
        orderService.updateOrderStatus(orderId, OrderStatus.PAYMENT_PROCESSING);
    }

    @KafkaListener(topics = "${kafka.topics.inventory-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onInventoryFailed(@Payload String payload,
                                @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        log.info("Inventory failed for orderId={}", orderId);
        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED);
    }

    @KafkaListener(topics = "${kafka.topics.payment-success}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentSuccess(String payload) {
        log.info("Received payment-success: {}", payload);
        // TODO Phase 4: update status → SHIPPING
    }

    @KafkaListener(topics = "${kafka.topics.payment-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(String payload) {
        log.info("Received payment-failed: {}", payload);
        // TODO Phase 4: update status → CANCELLED, publish inventory-release-command
    }

    @KafkaListener(topics = "${kafka.topics.shipping-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void onShippingCreated(String payload) {
        log.info("Received shipping-created: {}", payload);
        // TODO Phase 5: update status → COMPLETED
    }
}