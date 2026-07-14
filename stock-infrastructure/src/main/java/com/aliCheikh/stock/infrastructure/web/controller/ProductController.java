package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.GetProductStockLevelsQuery;
import com.aliCheikh.stock.application.dto.UpdateProductCommand;
import com.aliCheikh.stock.application.usecase.DeactivateProductUseCase;
import com.aliCheikh.stock.application.usecase.GetProductStockLevelsUseCase;
import com.aliCheikh.stock.application.usecase.SearchProductsUseCase;
import com.aliCheikh.stock.application.usecase.UpdateProductUseCase;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfProductResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ProductResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ProductSearchResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ProductStockSummaryResponse;
import com.aliCheikh.stock.infrastructure.web.dto.UpdateProductRequest;
import com.aliCheikh.stock.infrastructure.web.mapper.ProductWebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductController {

    private final ProductRepository productRepository;
    private final GetProductStockLevelsUseCase getProductStockLevelsUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeactivateProductUseCase deactivateProductUseCase;
    private final SearchProductsUseCase searchProductsUseCase;

    public ProductController(ProductRepository productRepository,
                             GetProductStockLevelsUseCase getProductStockLevelsUseCase,
                             UpdateProductUseCase updateProductUseCase,
                             DeactivateProductUseCase deactivateProductUseCase,
                             SearchProductsUseCase searchProductsUseCase) {
        this.productRepository = productRepository;
        this.getProductStockLevelsUseCase = getProductStockLevelsUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.deactivateProductUseCase = deactivateProductUseCase;
        this.searchProductsUseCase = searchProductsUseCase;
    }


    @GetMapping
    public ResponseEntity<PageOfProductResponse> getAllProducts(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
                                                                @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        List<Product> products = activeOnly
                ? productRepository.findAllActive(page, size)
                : productRepository.findAll(page, size);
        long totalElements = activeOnly
                ? productRepository.countActive()
                : productRepository.count();

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

    @PutMapping("/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID productId,
                                                         @Valid @RequestBody UpdateProductRequest request) {
        UpdateProductCommand updateProductCommand = ProductWebMapper.toCommand(productId, request);
        Product product = updateProductUseCase.execute(updateProductCommand);
        return ResponseEntity.ok(ProductWebMapper.toResponse(product));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable UUID productId) {
        deactivateProductUseCase.execute(ProductId.of(productId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductSearchResponse>> searchProducts(@RequestParam("q") String query) {
        List<ProductSearchResponse> results = searchProductsUseCase.findProductsByKeyword(query)
                .stream()
                .map(ProductWebMapper::toSearchResponse)
                .toList();
        return ResponseEntity.ok(results);
    }

}
