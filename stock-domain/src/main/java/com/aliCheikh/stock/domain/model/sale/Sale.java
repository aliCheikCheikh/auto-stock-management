package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.exception.sale.InvalidSaleException;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Sale {
    private final SaleId saleId;
    private final UserId soldBy;
    private final LocalDateTime occurredAt;
    private final Money totalAmount;
    private final List<SaleLineItem> lines;

    private Sale(SaleId saleId, UserId userId, LocalDateTime occurredAt, Money totalAmount, List<SaleLineItem> lines) {
        Objects.requireNonNull(saleId, "saleId cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(lines, "lines cannot be null");

        this.saleId = saleId;
        this.soldBy = userId;
        this.occurredAt = occurredAt;
        this.totalAmount = totalAmount;
        this.lines = List.copyOf(lines);
    }

    public static Sale create(UserId sellerId, List<SaleLineInput> lineRequests) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new InvalidSaleException(sellerId, "A sale must contain at least one line item");
        }

        List<SaleLineItem> internalLines = lineRequests.stream()
                .map(req -> new SaleLineItem(req.productId(), req.quantity(), req.unitPrice()))
                .toList();

        Money totalAmount = internalLines.stream()
                .map(SaleLineItem::getLineTotal)
                .reduce(Money::add)
                .orElseThrow(() -> new IllegalStateException("Sale.create invariant violated: lineRequests cannot be empty"));

        return new Sale(SaleId.generate(), sellerId, LocalDateTime.now(), totalAmount, internalLines);
    }


    /**
     * Internal Value Object, identified positionally by its parent Sale.
     * It has no independent existence outside the Sale aggregate.
     */
    private static final class SaleLineItem {
        private final ProductId productId;
        private final int quantity;
        private final Money unitPrice;
        private final Money lineTotal;

        private SaleLineItem(ProductId productId, int quantity, Money unitPrice) {
            Objects.requireNonNull(productId, "productId cannot be null");
            Objects.requireNonNull(unitPrice, "unitPrice cannot be null");

            if (quantity <= 0) {
                throw new IllegalArgumentException("Sale line quantity must be strictly positive");
            }
            if (!unitPrice.isPositive()) {
                throw new IllegalArgumentException("Sale line unit price must be strictly positive");
            }

            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = unitPrice.multiply(quantity);
        }

        public ProductId getProductId() {
            return productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public Money getUnitPrice() {
            return unitPrice;
        }

        public Money getLineTotal() {
            return lineTotal;
        }
    }

    public SaleId getSaleId() {
        return saleId;
    }

    public UserId getSoldBy() {
        return soldBy;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public List<SaleLineDto> getLines() {
        // TODO (Architecture): Recreating this DTO list on every read can impact performance for large collections.
        // Consider implementing a dedicated Read-Model (CQRS projection) if read scales.
        return this.lines.stream()
                .map(line -> new SaleLineDto(line.getProductId(), line.getQuantity(), line.getUnitPrice(), line.getLineTotal()))
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Sale sale = (Sale) o;
        return saleId.equals(sale.saleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saleId);
    }
}