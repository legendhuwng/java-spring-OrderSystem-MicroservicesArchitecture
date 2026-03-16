// ShippingService.java
package com.example.shipping_service.service;

import com.example.shipping_service.dto.PaymentSuccessEvent;
import com.example.shipping_service.entity.*;
import com.example.shipping_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String EVENT_TYPE = "payment-success";

    @Transactional
    public void createShipment(String orderId, PaymentSuccessEvent event) {

        // ── Idempotency check ──────────────────────────────────────
        if (processedEventRepository.existsByEventIdAndEventType(orderId, EVENT_TYPE)) {
            log.warn("Duplicate event, skipping: orderId={}", orderId);
            return;
        }

        log.info("Creating shipment for orderId={}", orderId);

        // ── Tạo shipment record ────────────────────────────────────
        String trackingNumber = "TRACK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .status(ShipmentStatus.CREATED)
                .trackingNumber(trackingNumber)
                .build();

        shipmentRepository.save(shipment);

        // ── Publish shipping-created ───────────────────────────────
        String payload = "{\"orderId\":\"" + orderId + "\","
                + "\"trackingNumber\":\"" + trackingNumber + "\","
                + "\"status\":\"CREATED\"}";

        kafkaTemplate.send("shipping-created", orderId, payload);
        log.info("Published shipping-created for orderId={}, tracking={}", orderId, trackingNumber);

        // ── Mark event as processed ────────────────────────────────
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(orderId)
                .eventType(EVENT_TYPE)
                .build());
    }
}