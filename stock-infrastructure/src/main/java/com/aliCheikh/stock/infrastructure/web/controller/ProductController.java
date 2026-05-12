package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfProductResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ProductResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.ProductWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }


    @GetMapping
    public ResponseEntity<PageOfProductResponse> getAllProducts(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        List<Product> products = productRepository.findAll(page, size);
        long totalElements = productRepository.count();
        PageOfProductResponse response = ProductWebMapper.toPageResponse(products, page, size, totalElements);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID productId) {
        return productRepository.findById(ProductId.of(productId))
                .map(ProductWebMapper::toResponse)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


}
