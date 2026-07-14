package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.ProductSearchView;
import com.aliCheikh.stock.application.port.ProductSearchQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Objects;

@Repository
public class ProductSearchQueryJpaAdapter implements ProductSearchQueryPort {
    private final ProductJpaRepository productJpaRepository;


    public ProductSearchQueryJpaAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = Objects.requireNonNull(productJpaRepository, "productJpaRepository cannot be null");

    }

    @Override
    public List<ProductSearchView> findProductsByKeyword(String keyword, int limit) {
        return productJpaRepository.searchActiveByKeyword(keyword, limit).
                stream()
                .map(this::toView)
                .toList();
    }

    private ProductSearchView toView(ProductJpaEntity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");
        return new ProductSearchView(
                ProductId.of(entity.getId()),
                entity.getName(),
                entity.getReference(),
                Money.create(entity.getUnitPriceAmount(),
                        Currency.getInstance(entity.getUnitPriceCurrency())));
    }
}
