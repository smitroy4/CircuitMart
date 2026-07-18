package com.smit.circuitmart.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEvent {
    private Long productId;
    private Integer quantity;
}