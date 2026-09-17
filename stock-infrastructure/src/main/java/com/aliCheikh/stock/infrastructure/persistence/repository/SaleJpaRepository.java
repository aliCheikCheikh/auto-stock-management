package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SaleJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {

    /**
     * Locks the sale until the transaction ends to serialize payments. Do not fetch-join both
     * saleLines and payments: their Cartesian product can duplicate payments in memory. The mapper
     * loads each collection separately within the same transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SaleJpaEntity s WHERE s.id = :saleId")
    Optional<SaleJpaEntity> findByIdForUpdate(@Param("saleId") UUID saleId);
}
