package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.StockLevelJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockLevelJpaId;
import com.aliCheikh.stock.infrastructure.persistence.projection.StockLevelRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface StockLevelQueryJpaRepository extends JpaRepository<StockLevelJpaEntity, StockLevelJpaId> {

    @Query("""
            select new com.aliCheikh.stock.infrastructure.persistence.projection.StockLevelRow(
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
            where (:productId is null or stockLevel.id.productId = :productId)
              and (:shopId is null or location.shopId = :shopId)
              and (:locationId is null or stockLevel.id.locationId = :locationId)
              and (
                  :belowThreshold = false
                  or stockLevel.id.productId in (
                      select thresholdStockLevel.id.productId
                      from StockLevelJpaEntity thresholdStockLevel
                      join ProductJpaEntity thresholdProduct on thresholdProduct.id = thresholdStockLevel.id.productId
                      group by thresholdStockLevel.id.productId, thresholdProduct.minimumGlobalThreshold
                      having sum(thresholdStockLevel.quantity) < thresholdProduct.minimumGlobalThreshold
                  )
              )
            """)
    Page<StockLevelRow> findByQuery(
            UUID productId,
            UUID shopId,
            UUID locationId,
            boolean belowThreshold,
            Pageable pageable
    );
}
