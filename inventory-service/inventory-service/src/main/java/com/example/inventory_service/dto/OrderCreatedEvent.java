// OrderCreatedEvent.java
package com.example.inventory_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderCreatedEvent {
    private String id;
    private List<OrderItemDto> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemDto {
        private String productId;
        private Integer quantity;
        private BigDecimal price;
    }
}