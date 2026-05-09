package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sale_line")
public class SaleLineJpaEntity {
    @EmbeddedId
    private SaleLineJpaId id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "quantity", nullable = false)
    private int quantity;
    @Column(name = "unit_price_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceAmount;
    @Column(name = "unit_price_currency", nullable = false, length = 3)
    private String unitPriceCurrency;
    @Column(name = "line_total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotalAmount;
    @Column(name = "line_total_currency", nullable = false, length = 3)
    private String lineTotalCurrency;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, insertable = false, updatable = false)
    private SaleJpaEntity sale;

    protected SaleLineJpaEntity() {
    }

    private SaleLineJpaEntity(
            SaleJpaEntity sale,
            int lineNumber,
            UUID productId,
            int quantity,
            BigDecimal unitPriceAmount,
            String unitPriceCurrency,
            BigDecimal lineTotalAmount,
            String lineTotalCurrency
    ) {
        Objects.requireNonNull(sale, "sale cannot be null");
        this.id = SaleLineJpaId.of(sale.getId(), lineNumber);
        this.sale = sale;
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be strictly positive");
        }

        this.quantity = quantity;
        this.unitPriceAmount = Objects.requireNonNull(unitPriceAmount, "unitPriceAmount cannot be null");
        this.unitPriceCurrency = Objects.requireNonNull(unitPriceCurrency, "unitPriceCurrency cannot be null");
        this.lineTotalAmount = Objects.requireNonNull(lineTotalAmount, "lineTotalAmount cannot be null");
        this.lineTotalCurrency = Objects.requireNonNull(lineTotalCurrency, "lineTotalCurrency cannot be null");

        if (this.unitPriceCurrency.isBlank()) {
            throw new IllegalArgumentException("unitPriceCurrency cannot be blank");
        }

        if (this.lineTotalCurrency.isBlank()) {
            throw new IllegalArgumentException("lineTotalCurrency cannot be blank");
        }

        if (!this.unitPriceCurrency.equals(this.lineTotalCurrency)) {
            throw new IllegalArgumentException("unitPriceCurrency and lineTotalCurrency must be equal");
        }
    }

    public static SaleLineJpaEntity of(SaleJpaEntity sale,
                                       int lineNumber,
                                       UUID productId,
                                       int quantity,
                                       BigDecimal unitPriceAmount,
                                       String unitPriceCurrency,
                                       BigDecimal lineTotalAmount,
                                       String lineTotalCurrency) {
        return new SaleLineJpaEntity(sale,
                lineNumber,
                productId,
                quantity,
                unitPriceAmount,
                unitPriceCurrency,
                lineTotalAmount,
                lineTotalCurrency);
    }

    public SaleLineJpaId getId() {
        return id;
    }

    public UUID getSaleId() {
        return id.getSaleId();
    }

    public int getLineNumber() {
        return id.getLineNumber();
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPriceAmount() {
        return unitPriceAmount;
    }

    public String getUnitPriceCurrency() {
        return unitPriceCurrency;
    }

    public BigDecimal getLineTotalAmount() {
        return lineTotalAmount;
    }

    public String getLineTotalCurrency() {
        return lineTotalCurrency;
    }

    public SaleJpaEntity getSale() {
        return sale;
    }
}
