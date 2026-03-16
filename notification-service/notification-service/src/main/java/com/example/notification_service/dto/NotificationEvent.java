// NotificationEvent.java
package com.example.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // bỏ qua field không cần
public class NotificationEvent {
    private String orderId;
    private String status;
    private String reason;
    private String trackingNumber;
}