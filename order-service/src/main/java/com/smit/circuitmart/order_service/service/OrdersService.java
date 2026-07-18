package com.smit.circuitmart.order_service.service;
import com.smit.circuitmart.order_service.client.InventoryFeignClient;
import com.smit.circuitmart.order_service.dto.OrderCreatedEvent;
import com.smit.circuitmart.order_service.dto.OrderItemEvent;
import com.smit.circuitmart.order_service.dto.OrderRequestDto;
import com.smit.circuitmart.order_service.entity.OrderItem;
import com.smit.circuitmart.order_service.entity.OrderStatus;
import com.smit.circuitmart.order_service.entity.Orders;
import com.smit.circuitmart.order_service.repository.OrdersRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdersService {

    private final OrdersRepository orderRepository;
    private final ModelMapper modelMapper;
    private final InventoryFeignClient inventoryFeignClient;
    private final StreamBridge streamBridge;

    public List<OrderRequestDto> getAllOrders() {
        log.info("Fetching all orders");
        List<Orders> orders = orderRepository.findAll();
        return orders.stream().map(order -> modelMapper.map(order, OrderRequestDto.class)).toList();
    }

    public OrderRequestDto getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);
        Orders order = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        return modelMapper.map(order, OrderRequestDto.class);
    }

    @CircuitBreaker(name = "inventoryCircuitBreaker", fallbackMethod = "createOrderFallback")
    public OrderRequestDto createOrder(OrderRequestDto orderRequestDto) {
        log.info("Calling the createOrder method");
        BigDecimal totalPrice = inventoryFeignClient.reduceStocks(orderRequestDto);
        if (totalPrice == null) {
            throw new RuntimeException("Inventory service returned null total price");
        }

        Orders orders = modelMapper.map(orderRequestDto, Orders.class);
        for(OrderItem orderItem: orders.getItems()) {
            orderItem.setOrder(orders);
        }
        orders.setTotalPrice(totalPrice);
        orders.setOrderStatus(OrderStatus.CONFIRMED);

        Orders savedOrder = orderRepository.save(orders);

        // Publish OrderCreatedEvent to Kafka
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(savedOrder.getId());
        event.setItems(savedOrder.getItems().stream()
            .map(item -> new OrderItemEvent(item.getProductId(), item.getQuantity()))
            .toList());
        streamBridge.send("orderCreatedEvent-out-0", event);
        log.info("Published OrderCreatedEvent for orderId: {}", savedOrder.getId());

        return modelMapper.map(savedOrder, OrderRequestDto.class);
    }

    public OrderRequestDto createOrderFallback(OrderRequestDto orderRequestDto, Throwable throwable){
        log.error("Fallback occurred due to : {}", throwable.getMessage());
        throw new RuntimeException("Order creation failed: " + throwable.getMessage());
    }

    @PostConstruct
    public void configureModelMapper() {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
    }

}







