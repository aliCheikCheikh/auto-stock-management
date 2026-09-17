package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.projection.SaleSettlementProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Settlement state for a batch of sales in one query. */
public interface SaleSettlementJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {

    /** A payment subquery keeps one row per sale, including sales with no payments. */
    @Query(value = """
            SELECT s.id             AS saleId,
                   s.total_amount   AS totalAmount,
                   s.total_currency AS currency,
                   COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.sale_id = s.id), 0) AS amountPaid
            FROM sale s
            WHERE s.id IN (:saleIds)
            """, nativeQuery = true)
    List<SaleSettlementProjection> findSettlements(@Param("saleIds") Collection<UUID> saleIds);
}
