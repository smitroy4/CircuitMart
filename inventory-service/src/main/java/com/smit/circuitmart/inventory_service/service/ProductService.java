package com.smit.circuitmart.inventory_service.service;
import com.smit.circuitmart.inventory_service.dto.OrderRequestDto;
import com.smit.circuitmart.inventory_service.dto.OrderRequestItemDto;
import com.smit.circuitmart.inventory_service.dto.ProductDto;
import com.smit.circuitmart.inventory_service.entity.Product;
import com.smit.circuitmart.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;

    public List<ProductDto> getAllInventory() {
        log.info("Fetching all inventory items");
        List<Product> inventories = productRepository.findAll();
        return inventories.stream()
                .map(product -> modelMapper.map(product, ProductDto.class))
                .toList();
    }

    public ProductDto getProductById(Long id) {
        log.info("Fetching Product with ID: {}", id);
        return productRepository.findById(id)
                .map(item -> modelMapper.map(item, ProductDto.class))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));
    }

    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reduceStocks(OrderRequestDto orderRequestDto) {
        log.info("Reducing the stocks");
        BigDecimal totalPrice = BigDecimal.ZERO;
        for(OrderRequestItemDto orderRequestItemDto: orderRequestDto.getItems()) {
            Long productId = orderRequestItemDto.getProductId();
            Integer quantity = orderRequestItemDto.getQuantity();

            Product product = productRepository.findById(productId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id: " + productId));

            if(product.getStock() < quantity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Product cannot be fulfilled for given quantity");
            }

            product.setStock(product.getStock()-quantity);
            productRepository.save(product);
            totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        return totalPrice;
    }
}

