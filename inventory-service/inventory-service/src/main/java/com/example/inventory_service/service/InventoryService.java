// InventoryService.java
package com.example.inventory_service.service;

import com.example.inventory_service.dto.OrderCreatedEvent;
import com.example.inventory_service.entity.*;
import com.example.inventory_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final InventoryReservationRepository reservationRepository; // NEW
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String EVENT_TYPE = "order-created";

    @Transactional
    public void reserveInventory(String orderId, OrderCreatedEvent event) {

        // ── Idempotency check ──────────────────────────────────────
        if (processedEventRepository.existsByEventIdAndEventType(orderId, EVENT_TYPE)) {
            log.warn("Duplicate event, skipping: orderId={}", orderId);
            return;
        }

        log.info("Processing inventory reservation for orderId={}", orderId);

        try {
            // ── Reserve stock cho từng item ────────────────────────
            for (OrderCreatedEvent.OrderItemDto item : event.getItems()) {
                Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                        .orElseThrow(() -> new RuntimeException(
                                "Product not found: " + item.getProductId()));

                if (inventory.getQuantity() < item.getQuantity()) {
                    throw new RuntimeException(
                            "Insufficient stock for product: " + item.getProductId()
                            + ", available: " + inventory.getQuantity()
                            + ", required: " + item.getQuantity());
                }

                inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
                inventoryRepository.save(inventory);

                // NEW: lưu lại đã reserve bao nhiêu để có thể rollback sau
                reservationRepository.save(InventoryReservation.builder()
                        .orderId(orderId)
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build());

                log.info("Reserved {} units of {} for orderId={}",
                        item.getQuantity(), item.getProductId(), orderId);
            }

            // ── Publish inventory-reserved ─────────────────────────
            kafkaTemplate.send("inventory-reserved", orderId,
                    "{\"orderId\":\"" + orderId + "\",\"status\":\"RESERVED\"}");
            log.info("Published inventory-reserved for orderId={}", orderId);

        } catch (RuntimeException e) {
            log.error("Inventory reservation failed for orderId={}: {}", orderId, e.getMessage());

            // ── Publish inventory-failed ───────────────────────────
            kafkaTemplate.send("inventory-failed", orderId,
                    "{\"orderId\":\"" + orderId + "\",\"reason\":\"" + e.getMessage() + "\"}");
        }

        // ── Mark event as processed (dù success hay fail) ─────────
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(orderId)
                .eventType(EVENT_TYPE)
                .build());
    }

    // Dùng để seed data test
    @Transactional
    public Inventory addStock(String productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElse(Inventory.builder().productId(productId).quantity(0).build());
        inventory.setQuantity(inventory.getQuantity() + quantity);
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void releaseInventory(String orderId, String payload) {
    // Idempotency
        if (processedEventRepository.existsByEventIdAndEventType(orderId, "inventory-release-command")) {
            log.warn("Duplicate release event, skipping: orderId={}", orderId);
            return;
        }

        log.info("Releasing inventory for orderId={}", orderId);

        // NEW: query đúng số lượng đã reserve, cộng lại vào stock
        List<InventoryReservation> reservations =
                reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            log.warn("No reservations found for orderId={}, nothing to release", orderId);
        }

        for (InventoryReservation reservation : reservations) {
            Inventory inventory = inventoryRepository.findByProductId(reservation.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found: " + reservation.getProductId()));

            inventory.setQuantity(inventory.getQuantity() + reservation.getQuantity());
            inventoryRepository.save(inventory);

            reservation.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(reservation);

            log.info("Released {} units of {} for orderId={}",
                    reservation.getQuantity(), reservation.getProductId(), orderId);
        }

        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(orderId)
                .eventType("inventory-release-command")
                .build());

        log.info("Inventory release completed for orderId={}", orderId);
    }

}