package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "sale")
public class SaleJpaEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "sold_by", nullable = false)
    private UUID soldBy;
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "total_currency", nullable = false, length = 3)
    private String totalCurrency;
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<SaleLineJpaEntity> saleLines = new HashSet<>();

    protected SaleJpaEntity() {
    }

    private SaleJpaEntity(UUID id, UUID soldBy, LocalDateTime occurredAt, BigDecimal totalAmount, String totalCurrency) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.soldBy = Objects.requireNonNull(soldBy, "soldBy cannot be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        this.totalCurrency = Objects.requireNonNull(totalCurrency, "totalCurrency cannot be null");
    }

    public static SaleJpaEntity of(UUID id, UUID soldBy, LocalDateTime occurredAt, BigDecimal totalAmount, String totalCurrency) {
        return new SaleJpaEntity(id, soldBy, occurredAt, totalAmount, totalCurrency);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSoldBy() {
        return soldBy;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getTotalCurrency() {
        return totalCurrency;
    }

    public Set<SaleLineJpaEntity> getSaleLines() {
        return saleLines;
    }

    public void replaceSaleLines(Set<SaleLineJpaEntity> saleLines) {
        this.saleLines.clear();
        this.saleLines.addAll(Objects.requireNonNull(saleLines, "saleLines cannot be null"));
    }
}
