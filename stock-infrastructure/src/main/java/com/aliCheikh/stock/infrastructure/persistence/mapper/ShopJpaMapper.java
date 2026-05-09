package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.shop.Shop;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.infrastructure.persistence.entity.ShopJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ShopJpaMapper {

    public Shop toDomain(ShopJpaEntity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return new Shop(
                ShopId.of(entity.getId()),
                entity.getName(),
                entity.getAddress()
        );
    }

    public ShopJpaEntity toEntity(Shop shop) {
        Objects.requireNonNull(shop, "shop cannot be null");

        return ShopJpaEntity.of(
                shop.getShopId().getValue(),
                shop.getName(),
                shop.getAddress()
        );
    }
}
