// ProcessedEventRepository.java
package com.example.shipping_service.repository;

import com.example.shipping_service.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    boolean existsByEventIdAndEventType(String eventId, String eventType);
}