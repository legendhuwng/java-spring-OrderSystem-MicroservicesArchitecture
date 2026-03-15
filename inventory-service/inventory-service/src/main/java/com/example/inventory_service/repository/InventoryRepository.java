// InventoryRepository.java
package com.example.inventory_service.repository;

import com.example.inventory_service.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {

    @Lock(LockModeType.OPTIMISTIC)
    Optional<Inventory> findByProductId(String productId);
}