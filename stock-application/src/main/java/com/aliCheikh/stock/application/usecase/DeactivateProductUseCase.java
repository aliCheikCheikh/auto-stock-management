package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;

import java.util.Objects;


public class DeactivateProductUseCase {
    private final ProductRepository productRepository;

    public DeactivateProductUseCase(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
    }

    public void execute(ProductId productId) {
        Objects.requireNonNull(productId, "productId cannot be null");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.deactivate();
        productRepository.save(product);

    }
}
