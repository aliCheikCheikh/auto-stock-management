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
    @Column(name = "customer_id")
    private UUID customerId;
    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;
    @Column(name = "amount_paid_currency", nullable = false, length = 3)
    private String amountPaidCurrency;

    protected SaleJpaEntity() {
    }

    private SaleJpaEntity(UUID id,
                          UUID soldBy,
                          LocalDateTime occurredAt,
                          BigDecimal totalAmount,
                          String totalCurrency,
                          UUID customerId,
                          BigDecimal amountPaid,
                          String amountPaidCurrency) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.soldBy = Objects.requireNonNull(soldBy, "soldBy cannot be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        this.totalCurrency = Objects.requireNonNull(totalCurrency, "totalCurrency cannot be null");
        // customerId reste nullable : une vente au comptant n'a pas de client rattaché.
        this.customerId = customerId;
        this.amountPaid = Objects.requireNonNull(amountPaid, "amountPaid cannot be null");
        this.amountPaidCurrency = Objects.requireNonNull(amountPaidCurrency, "amountPaidCurrency cannot be null");
    }

    /**
     * Vente à crédit ou au comptant : l'appelant fournit explicitement le client (éventuellement
     * {@code null}) et le montant encaissé.
     */
    public static SaleJpaEntity of(UUID id,
                                   UUID soldBy,
                                   LocalDateTime occurredAt,
                                   BigDecimal totalAmount,
                                   String totalCurrency,
                                   UUID customerId,
                                   BigDecimal amountPaid,
                                   String amountPaidCurrency) {
        return new SaleJpaEntity(id, soldBy, occurredAt, totalAmount, totalCurrency,
                customerId, amountPaid, amountPaidCurrency);
    }

    /**
     * Vente au comptant : intégralement payée, sans client rattaché.
     *
     * <p>Surcharge de commodité qui préserve les appelants antérieurs à la gestion des créances.</p>
     */
    public static SaleJpaEntity of(UUID id,
                                   UUID soldBy,
                                   LocalDateTime occurredAt,
                                   BigDecimal totalAmount,
                                   String totalCurrency) {
        return new SaleJpaEntity(id, soldBy, occurredAt, totalAmount, totalCurrency,
                null, totalAmount, totalCurrency);
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

    public UUID getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public String getAmountPaidCurrency() {
        return amountPaidCurrency;
    }

    public void replaceSaleLines(Set<SaleLineJpaEntity> saleLines) {
        this.saleLines.clear();
        this.saleLines.addAll(Objects.requireNonNull(saleLines, "saleLines cannot be null"));
    }
}
