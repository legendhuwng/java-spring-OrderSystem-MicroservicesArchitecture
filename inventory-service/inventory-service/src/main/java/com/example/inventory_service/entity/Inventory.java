package com.example.inventory_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "INVENTORY")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {

    @Id
    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Version
    @Column(name = "VERSION")
    private Long version;  // optimistic locking – tránh race condition
}