// OutboxScheduler.java
package com.example.order_service.scheduler;

import com.example.order_service.entity.OutboxEvent;
import com.example.order_service.entity.OutboxStatus;
import com.example.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)  // chạy mỗi 5 giây
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload());
                event.setStatus(OutboxStatus.PUBLISHED);
                outboxEventRepository.save(event);
                log.info("Published outbox event: type={}, aggregateId={}", event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                event.setStatus(OutboxStatus.FAILED);
                outboxEventRepository.save(event);
                log.error("Failed to publish outbox event: id={}", event.getId(), e);
            }
        }
    }
}