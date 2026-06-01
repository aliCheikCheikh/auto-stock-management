package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.StockLevel;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockLevelJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StorageLocationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StorageLocationJpaMapper {

    public StorageLocation toDomain(StorageLocationJpaEntity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        Map<ProductId, StockLevel> stockLevels = new HashMap<>();

        for (StockLevelJpaEntity stockLevelEntity : entity.getStockLevels()) {
            ProductId productId = ProductId.of(stockLevelEntity.getProductId());
            stockLevels.put(
                    productId,
                    StockLevel.of(productId, stockLevelEntity.getQuantity())
            );
        }

        return StorageLocation.rehydrate(
                LocationId.of(entity.getId()),
                ShopId.of(entity.getShopId()),
                entity.getLocationType(),
                entity.getLabel(),
                entity.getLowStockIndicator(),
                stockLevels
        );
    }

    public StorageLocationJpaEntity toEntity(StorageLocation location) {
        Objects.requireNonNull(location, "location cannot be null");

        StorageLocationJpaEntity entity = StorageLocationJpaEntity.of(
                location.getLocationId().getValue(),
                location.getShopId().getValue(),
                location.getLocationType(),
                location.getLabel(),
                location.getLowStockIndicator()
        );

        Set<StockLevelJpaEntity> stockLevelEntities = new HashSet<>();

        for (Map.Entry<ProductId, StockLevel> entry : location.getStockLevels().entrySet()) {
            stockLevelEntities.add(StockLevelJpaEntity.of(
                    entity,
                    entry.getKey().getValue(),
                    entry.getValue().getQuantity()
            ));
        }

        entity.replaceStockLevels(stockLevelEntities);
        return entity;
    }

    public void updateEntity(StorageLocation source, StorageLocationJpaEntity target) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(target, "target cannot be null");

        Set<StockLevelJpaEntity> stockLevelEntities = new HashSet<>();

        for (Map.Entry<ProductId, StockLevel> entry : source.getStockLevels().entrySet()) {
            stockLevelEntities.add(StockLevelJpaEntity.of(
                    target,
                    entry.getKey().getValue(),
                    entry.getValue().getQuantity()
            ));
        }

        target.replaceStockLevels(stockLevelEntities);
    }
}