package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.infrastructure.persistence.mapper.ProductJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Override
    public List<Product> findAll(int page, int size) {
        return productJpaRepository.findAll(PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(productJpaMapper::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return productJpaRepository.count();
    }

    @Override
    public List<Product> findAllActive(int page, int size) {
        return productJpaRepository.findByActiveTrue(PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(productJpaMapper::toDomain)
                .toList();
    }

    @Override
    public long countActive() {
        return productJpaRepository.countByActiveTrue();
    }


    @Override
    public boolean existsByName(String name) {
        return productJpaRepository.existsByName(name);
    }

    @Override
    public boolean existsByCategoryId(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId cannot be null");
        return productJpaRepository.existsByCategoryId(categoryId.getValue());
    }
}
