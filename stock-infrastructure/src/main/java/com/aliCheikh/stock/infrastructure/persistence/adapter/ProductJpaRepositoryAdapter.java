package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.infrastructure.persistence.mapper.ProductJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

@Repository
public class ProductJpaRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;
    private final ProductJpaMapper productJpaMapper;

    public ProductJpaRepositoryAdapter(
            ProductJpaRepository productJpaRepository,
            ProductJpaMapper productJpaMapper
    ) {
        this.productJpaRepository = Objects.requireNonNull(productJpaRepository, "productJpaRepository cannot be null");
        this.productJpaMapper = Objects.requireNonNull(productJpaMapper, "productJpaMapper cannot be null");
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        Objects.requireNonNull(productId, "productId cannot be null");

        return productJpaRepository.findById(productId.getValue())
                .map(productJpaMapper::toDomain);
    }

    @Override
    public Optional<Product> findByReference(String reference) {
        Objects.requireNonNull(reference, "reference cannot be null");

        if (reference.isBlank()) {
            throw new IllegalArgumentException("reference cannot be blank");
        }

        return productJpaRepository.findByReference(reference)
                .map(productJpaMapper::toDomain);
    }

    @Override
    public void save(Product product) {
        Objects.requireNonNull(product, "product cannot be null");

        productJpaRepository.save(productJpaMapper.toEntity(product));
    }
}
