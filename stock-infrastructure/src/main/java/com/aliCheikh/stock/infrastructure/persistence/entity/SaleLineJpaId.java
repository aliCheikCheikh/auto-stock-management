package com.aliCheikh.stock.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SaleLineJpaId implements Serializable {
    @Column(name = "sale_id", nullable = false)
    private UUID saleId;
    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    protected SaleLineJpaId() {
    }

    private SaleLineJpaId(UUID saleId, int lineNumber) {
        this.saleId = Objects.requireNonNull(saleId, "saleId cannot be null");
        if (lineNumber <= 0) {
            throw new IllegalArgumentException("lineNumber must be strictly positive");
        }
        this.lineNumber = lineNumber;
    }

    public static SaleLineJpaId of(UUID saleId, int lineNumber) {
        return new SaleLineJpaId(saleId, lineNumber);
    }

    public UUID getSaleId() {
        return saleId;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SaleLineJpaId that = (SaleLineJpaId) o;
        return lineNumber == that.lineNumber && Objects.equals(saleId, that.saleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleId, lineNumber);
    }

}
