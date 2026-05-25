package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.GetProductStockLevelsQuery;
import com.aliCheikh.stock.application.usecase.GetProductStockLevelsUseCase;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfProductResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ProductResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ProductStockSummaryResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.ProductWebMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductController {

    private final ProductRepository productRepository;
    private final GetProductStockLevelsUseCase getProductStockLevelsUseCase;

    public ProductController(ProductRepository productRepository, GetProductStockLevelsUseCase getProductStockLevelsUseCase) {
        this.productRepository = productRepository;
        this.getProductStockLevelsUseCase = getProductStockLevelsUseCase;
    }


    @GetMapping
    public ResponseEntity<PageOfProductResponse> getAllProducts(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        List<Product> products = productRepository.findAll(page, size);
        long totalElements = productRepository.count();
        PageOfProductResponse response = ProductWebMapper.toPageResponse(products, page, size, totalElements);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID productId) {
        ProductId id = ProductId.of(productId);
        Product product = productRepository
                .findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ResponseEntity.ok(ProductWebMapper.toResponse(product));
    }

    @GetMapping("/{productId}/stock-levels")
    public ResponseEntity<ProductStockSummaryResponse> getProductStockLevels(@PathVariable UUID productId,
                                                                             @RequestParam(required = false) UUID shopId) {
        GetProductStockLevelsQuery query = new GetProductStockLevelsQuery(ProductId.of(productId),
                shopId == null ? null : ShopId.of(shopId));

        return getProductStockLevelsUseCase.execute(query)
                .map(ProductWebMapper::toProductStockSummaryResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ProductNotFoundException(ProductId.of(productId)));

    }

}
