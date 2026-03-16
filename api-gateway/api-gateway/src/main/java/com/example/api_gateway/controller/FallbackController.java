package com.example.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/order-service")
    public ResponseEntity<Map<String, String>> orderServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Order Service is currently unavailable. Please try again later.",
                        "service", "order-service"
                ));
    }

    @RequestMapping("/inventory-service")
    public ResponseEntity<Map<String, String>> inventoryServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Inventory Service is currently unavailable. Please try again later.",
                        "service", "inventory-service"
                ));
    }

    @RequestMapping("/payment-service")
    public ResponseEntity<Map<String, String>> paymentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Payment Service is currently unavailable. Please try again later.",
                        "service", "payment-service"
                ));
    }

    @RequestMapping("/shipping-service")
    public ResponseEntity<Map<String, String>> shippingServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Shipping Service is currently unavailable. Please try again later.",
                        "service", "shipping-service"
                ));
    }
}