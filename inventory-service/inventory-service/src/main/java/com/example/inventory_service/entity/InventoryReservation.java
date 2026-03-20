package com.example.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "INVENTORY_RESERVATIONS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private String id;

    @Column(name = "ORDER_ID", nullable = false)
    private String orderId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private String productId;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        status = ReservationStatus.RESERVED;
    }
}