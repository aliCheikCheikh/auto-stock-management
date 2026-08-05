package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.StockReceiptImportExecutionJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface StockReceiptImportExecutionJpaRepository
        extends JpaRepository<StockReceiptImportExecutionJpaEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO stock_receipt_import_execution(
                import_id, fingerprint, shop_id, user_id, created_at
            ) VALUES (
                :importId, :fingerprint, :shopId, :userId, :createdAt
            )
            ON CONFLICT (import_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("importId") UUID importId,
            @Param("fingerprint") String fingerprint,
            @Param("shopId") UUID shopId,
            @Param("userId") UUID userId,
            @Param("createdAt") Instant createdAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT execution FROM StockReceiptImportExecutionJpaEntity execution "
            + "WHERE execution.importId = :importId")
    Optional<StockReceiptImportExecutionJpaEntity> findByIdForUpdate(@Param("importId") UUID importId);
}
