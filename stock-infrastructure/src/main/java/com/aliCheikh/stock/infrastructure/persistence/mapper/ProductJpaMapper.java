package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Objects;

@Component
public class ProductJpaMapper {

    public Product toDomain(ProductJpaEntity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return new Product(
                ProductId.of(entity.getId()),
                entity.getName(),
                entity.getReference(),
                CategoryId.of(entity.getCategoryId()),
                entity.getMinimumGlobalThreshold(),
                Money.create(
                        entity.getUnitPriceAmount(),
                        Currency.getInstance(entity.getUnitPriceCurrency())
                )
        );
    }

    public ProductJpaEntity toEntity(Product product) {
        Objects.requireNonNull(product, "product cannot be null");

        return ProductJpaEntity.of(
                product.getProductId().getValue(),
                product.getName(),
                product.getReference(),
                product.getCategoryId().getValue(),
                product.getMinimumGlobalThreshold(),
                product.getUnitPrice().getAmount(),
                product.getUnitPrice().getCurrency().getCurrencyCode()
        );
    }
}
