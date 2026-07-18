package com.smit.circuitmart.order_service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequestDto {
    private List<OrderRequestItemDto> items;
}

