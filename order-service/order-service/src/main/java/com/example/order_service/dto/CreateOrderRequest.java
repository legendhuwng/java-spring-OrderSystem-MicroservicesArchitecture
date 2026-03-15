// CreateOrderRequest.java
package com.example.order_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateOrderRequest {

    @NotEmpty
    private List<OrderItemRequest> items;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemRequest {
        @NotBlank
        private String productId;

        @Min(1)
        private Integer quantity;

        @NotNull @DecimalMin("0.0")
        private BigDecimal price;
    }
}