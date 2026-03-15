// OutboxEventRepository.java
package com.example.order_service.repository;

import com.example.order_service.entity.OutboxEvent;
import com.example.order_service.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findByStatus(OutboxStatus status);
}