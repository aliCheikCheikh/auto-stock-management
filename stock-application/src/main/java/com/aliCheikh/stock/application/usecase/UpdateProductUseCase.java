package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.UpdateProductCommand;
import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;

import java.util.Objects;

public class UpdateProductUseCase {
    private final ProductRepository productRepository;

    public UpdateProductUseCase(ProductRepository productRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
    }

    public Product execute(UpdateProductCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        Product product = productRepository.findById(command.productId())
                .orElseThrow(() -> new ProductNotFoundException(command.productId()));
        product.rename(command.name());
        product.updatePrice(command.unitPrice());
        product.updateThreshold(command.minimumGlobalThreshold());
        productRepository.save(product);
        return product;
    }
}
