package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.StockLevelJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockLevelJpaId;
import com.aliCheikh.stock.infrastructure.persistence.projection.ProductStockLevelRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductStockQueryJpaRepository extends JpaRepository<StockLevelJpaEntity, StockLevelJpaId> {

    @Query("""
            select new com.aliCheikh.stock.infrastructure.persistence.projection.ProductStockLevelRow(
                stockLevel.id.productId,
                product.name,
                stockLevel.id.locationId,
                location.label,
                location.locationType,
                location.shopId,
                stockLevel.quantity
            )
            from StockLevelJpaEntity stockLevel
            join stockLevel.location location
            join ProductJpaEntity product on product.id = stockLevel.id.productId
            where stockLevel.id.productId = :productId
              and (:shopId is null or location.shopId = :shopId)
            """)
    List<ProductStockLevelRow> findStockLevelsByProductId(
            UUID productId,
            UUID shopId
    );
}