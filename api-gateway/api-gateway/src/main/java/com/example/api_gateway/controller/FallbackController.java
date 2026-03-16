package com.example.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/order-service")
    @PostMapping("/order-service")
    public ResponseEntity<Map<String, String>> orderServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Order Service is currently unavailable. Please try again later.",
                        "service", "order-service"
                ));
    }

    @GetMapping("/inventory-service")
    @PostMapping("/inventory-service")
    public ResponseEntity<Map<String, String>> inventoryServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Inventory Service is currently unavailable. Please try again later.",
                        "service", "inventory-service"
                ));
    }

    @GetMapping("/payment-service")
    @PostMapping("/payment-service")
    public ResponseEntity<Map<String, String>> paymentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Payment Service is currently unavailable. Please try again later.",
                        "service", "payment-service"
                ));
    }

    @GetMapping("/shipping-service")
    @PostMapping("/shipping-service")
    public ResponseEntity<Map<String, String>> shippingServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "error",
                        "message", "Shipping Service is currently unavailable. Please try again later.",
                        "service", "shipping-service"
                ));
    }
}