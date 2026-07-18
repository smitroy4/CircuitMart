package com.smit.circuitmart.inventory_service.consumer;

import com.smit.circuitmart.inventory_service.dto.OrderRequestDto;
import com.smit.circuitmart.inventory_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final ProductService productService;

    @Bean
    public Consumer<OrderRequestDto> reserveStock() {
        return orderRequest -> {
            log.info("Received OrderCreatedEvent: {}", orderRequest);
            productService.reduceStocks(orderRequest);
        };
    }
}