package com.example.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Lấy header từ request
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);

        // Nếu không có thì tạo mới
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Biến final để dùng trong lambda
        final String finalCorrelationId = correlationId;

        // Mutate request với header mới
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(CORRELATION_ID, finalCorrelationId))
                .build();

        // Đảm bảo header response được thêm trước khi commit
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().add(CORRELATION_ID, finalCorrelationId);
            return Mono.empty();
        });

        // Tiếp tục filter chain với request đã mutate
        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        // Giá trị âm để filter chạy sớm
        return -1;
    }
}