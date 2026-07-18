package com.smit.circuitmart.order_service.client;

import com.smit.circuitmart.order_service.dto.OrderRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "inventory-service", path = "/inventory")
public interface InventoryFeignClient {

    @PostMapping("/products/reduce-stocks")
    BigDecimal reduceStocks(@RequestBody OrderRequestDto orderRequestDto);

}
