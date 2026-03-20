// InventoryController.java
package com.example.inventory_service.controller;

import com.example.inventory_service.entity.Inventory;
import com.example.inventory_service.repository.InventoryRepository;
import com.example.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    @GetMapping("/{productId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Inventory> getStock(@PathVariable String productId) {
        return inventoryRepository.findByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{productId}/stock")
    public ResponseEntity<Inventory> addStock(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.addStock(productId, quantity));
    }
}