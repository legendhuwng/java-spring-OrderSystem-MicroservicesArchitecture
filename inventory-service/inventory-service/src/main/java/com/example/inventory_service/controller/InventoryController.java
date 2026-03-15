// InventoryController.java
package com.example.inventory_service.controller;

import com.example.inventory_service.entity.Inventory;
import com.example.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // Seed stock để test
    @PostMapping("/{productId}/stock")
    public ResponseEntity<Inventory> addStock(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.addStock(productId, quantity));
    }
}