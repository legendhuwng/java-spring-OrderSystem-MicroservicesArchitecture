// InventoryReservedEvent.java
package com.example.payment_service.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InventoryReservedEvent {
    private String orderId;
    private String status;
}