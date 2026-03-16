package com.example.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PROCESSED_EVENTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProcessedEvent {

    @Id
    @Column(name = "EVENT_ID")
    private String eventId;

    @Column(name = "EVENT_TYPE")
    private String eventType;

    @Column(name = "PROCESSED_AT")
    private LocalDateTime processedAt;

    @PrePersist
    void prePersist() {
        processedAt = LocalDateTime.now();
    }
}