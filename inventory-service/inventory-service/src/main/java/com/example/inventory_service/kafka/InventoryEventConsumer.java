// InventoryEventConsumer.java
package com.example.inventory_service.kafka;

import com.example.inventory_service.dto.OrderCreatedEvent;
import com.example.inventory_service.service.InventoryService;
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
public class InventoryEventConsumer {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = "${kafka.topics.order-created}",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onOrderCreated(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String orderId) throws Exception{
        log.info("Received order-created event: orderId={}", orderId);
        // BỎ try-catch — để exception bubble up → DLQ handler bắt
        OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        inventoryService.reserveInventory(orderId, event);
    }

@KafkaListener(
    topics = "inventory-release-command",
    groupId = "${spring.kafka.consumer.group-id}"
)
public void onInventoryReleaseCommand(
        @Payload String payload,
        @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
    log.info("Received inventory-release-command: orderId={}", orderId);
    inventoryService.releaseInventory(orderId, payload);
}

}