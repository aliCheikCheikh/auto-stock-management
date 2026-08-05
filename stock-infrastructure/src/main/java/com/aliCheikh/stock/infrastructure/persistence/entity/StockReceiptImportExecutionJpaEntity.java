package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_receipt_import_execution")
public class StockReceiptImportExecutionJpaEntity {

    @Id
    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "fingerprint", nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockReceiptImportExecutionJpaEntity() {
    }

    public boolean matches(String expectedFingerprint, UUID expectedShopId, UUID expectedUserId) {
        return fingerprint.equals(expectedFingerprint)
                && shopId.equals(expectedShopId)
                && userId.equals(expectedUserId);
    }

    public UUID getImportId() {
        return importId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public UUID getShopId() {
        return shopId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
