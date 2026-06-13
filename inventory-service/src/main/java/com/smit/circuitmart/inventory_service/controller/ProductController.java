package com.smit.circuitmart.inventory_service.controller;

import com.smit.circuitmart.inventory_service.dto.ProductDto;
import com.smit.circuitmart.inventory_service.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;

    @GetMapping("/fetchOrders")
    public String fetchFromOrdersService(){
        ServiceInstance orderService = discoveryClient.getInstances("order-service").getFirst();

        return restClient.get()
                .uri(orderService.getUri()+"/orders/orders/helloOrders")
                .retrieve()
                .body(String.class);
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllInventory() {
        List<ProductDto> inventories = productService.getAllInventory();
        return ResponseEntity.ok(inventories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getInventoryById(@PathVariable Long id) {
        ProductDto inventory = productService.getProductById(id);
        return ResponseEntity.ok(inventory);
    }

//    @PutMapping("reduce-stocks")
//    public ResponseEntity<Double> reduceStocks(@RequestBody OrderRequestDto orderRequestDto) {
//        Double totalPrice = productService.reduceStocks(orderRequestDto);
//        return ResponseEntity.ok(totalPrice);
//    }

}
