package com.smit.circuitmart.inventory_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "order-service", path = "/orders")
public interface OrdersFeignClient {

    @GetMapping("/helloOrders")
    String helloOrders();

}
