package com.example.order_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OUTBOX_EVENTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private String id;

    @Column(name = "AGGREGATE_TYPE", nullable = false)
    private String aggregateType;   // "ORDER"

    @Column(name = "AGGREGATE_ID", nullable = false)
    private String aggregateId;     // orderId

    @Column(name = "EVENT_TYPE", nullable = false)
    private String eventType;       // "order-created"

    @Column(name = "PAYLOAD", nullable = false, length = 4000)
    private String payload;         // JSON

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        status = OutboxStatus.PENDING;
    }
}