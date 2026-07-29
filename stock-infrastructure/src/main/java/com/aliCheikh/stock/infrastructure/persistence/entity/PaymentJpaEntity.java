package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Un encaissement rattaché à une vente.
 *
 * <p>Identifiant technique propre à la ligne : contrairement aux lignes de vente, un paiement n'a
 * pas de numéro d'ordre métier, et deux encaissements du même montant le même jour restent deux
 * faits distincts.</p>
 */
@Entity
@Table(name = "payment")
public class PaymentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "received_by", nullable = false)
    private UUID receivedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private SaleJpaEntity sale;

    protected PaymentJpaEntity() {
    }

    private PaymentJpaEntity(UUID id,
                             SaleJpaEntity sale,
                             BigDecimal amount,
                             String currency,
                             LocalDateTime receivedAt,
                             UUID receivedBy) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.sale = Objects.requireNonNull(sale, "sale cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "currency cannot be null");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt cannot be null");
        this.receivedBy = Objects.requireNonNull(receivedBy, "receivedBy cannot be null");
    }

    public static PaymentJpaEntity of(UUID id,
                                      SaleJpaEntity sale,
                                      BigDecimal amount,
                                      String currency,
                                      LocalDateTime receivedAt,
                                      UUID receivedBy) {
        return new PaymentJpaEntity(id, sale, amount, currency, receivedAt, receivedBy);
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public UUID getReceivedBy() {
        return receivedBy;
    }

    public SaleJpaEntity getSale() {
        return sale;
    }
}
