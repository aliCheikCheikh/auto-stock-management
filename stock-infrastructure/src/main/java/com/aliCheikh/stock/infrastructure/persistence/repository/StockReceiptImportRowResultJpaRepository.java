package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.StockReceiptImportRowResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockReceiptImportRowResultJpaRepository
        extends JpaRepository<StockReceiptImportRowResultJpaEntity, UUID> {

    Optional<StockReceiptImportRowResultJpaEntity> findByImportIdAndLineNumber(
            UUID importId,
            int lineNumber
    );
}
