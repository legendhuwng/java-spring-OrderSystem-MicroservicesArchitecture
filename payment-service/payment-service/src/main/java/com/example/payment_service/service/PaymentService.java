// PaymentService.java
package com.example.payment_service.service;

import com.example.payment_service.dto.InventoryReservedEvent;
import com.example.payment_service.entity.*;
import com.example.payment_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${payment.failure-rate:0.3}")
    private double failureRate;

    private static final String EVENT_TYPE = "inventory-reserved";
    private final Random random = new Random();

    @Transactional
    public void processPayment(String orderId, InventoryReservedEvent event) {

        // ── Idempotency check ──────────────────────────────────────
        if (processedEventRepository.existsByEventIdAndEventType(orderId, EVENT_TYPE)) {
            log.warn("Duplicate event, skipping: orderId={}", orderId);
            return;
        }

        log.info("Processing payment for orderId={}", orderId);

        // ── Fake payment logic ─────────────────────────────────────
        boolean success = random.nextDouble() > failureRate;

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(BigDecimal.valueOf(100000)) // mock amount
                .status(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .build();

        paymentRepository.save(payment);

        if (success) {
            kafkaTemplate.send("payment-success", orderId,
                    "{\"orderId\":\"" + orderId + "\",\"status\":\"SUCCESS\"}");
            log.info("Payment SUCCESS for orderId={}", orderId);
        } else {
            kafkaTemplate.send("payment-failed", orderId,
                    "{\"orderId\":\"" + orderId + "\",\"reason\":\"Insufficient funds\"}");
            log.warn("Payment FAILED for orderId={}", orderId);
        }

        // ── Mark event as processed ────────────────────────────────
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(orderId)
                .eventType(EVENT_TYPE)
                .build());
    }
}