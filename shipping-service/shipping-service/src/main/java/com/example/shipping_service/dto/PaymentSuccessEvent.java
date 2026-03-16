// PaymentSuccessEvent.java
package com.example.shipping_service.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PaymentSuccessEvent {
    private String orderId;
    private String status;
}