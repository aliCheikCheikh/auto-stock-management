package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.exception.sale.CreditSaleRequiresCustomerException;
import com.aliCheikh.stock.domain.exception.sale.InvalidSaleException;
import com.aliCheikh.stock.domain.exception.sale.PaymentExceedsAmountDueException;
import com.aliCheikh.stock.domain.exception.sale.SaleAlreadySettledException;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Sale aggregate containing immutable line items and an accumulating payment ledger. Keeping both
 * in the same consistency boundary allows overpayment checks. Amount paid and amount due are
 * derived from the ledger. An outstanding balance requires an identified customer.
 */
public class Sale {

    private final SaleId saleId;
    private final UserId soldBy;
    private final LocalDateTime occurredAt;
    private final Money totalAmount;
    private final List<SaleLineItem> lines;

    /** Debtor; optional for a fully paid sale, required when a balance remains. */
    private final CustomerId customerId;

    /** Payments ordered oldest first. */
    private final List<Payment> payments;

    /** Validates invariants shared by all construction paths. */
    private Sale(SaleId saleId,
                 UserId soldBy,
                 LocalDateTime occurredAt,
                 Money totalAmount,
                 List<SaleLineItem> lines,
                 CustomerId customerId,
                 List<Payment> payments) {
        Objects.requireNonNull(saleId, "saleId cannot be null");
        Objects.requireNonNull(soldBy, "soldBy cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        Objects.requireNonNull(lines, "lines cannot be null");
        Objects.requireNonNull(payments, "payments cannot be null");

        Money collected = sumOfPayments(payments, totalAmount.getCurrency());
        requireConsistentPaymentTerms(soldBy, totalAmount, collected, customerId);

        this.saleId = saleId;
        this.soldBy = soldBy;
        this.occurredAt = occurredAt;
        this.totalAmount = totalAmount;
        this.lines = List.copyOf(lines);
        this.customerId = customerId;
        this.payments = new ArrayList<>(payments);
    }

    /**
     * Creates a sale with at least one line. {@code amountPaid} defaults to the full total when
     * null; an outstanding balance requires {@code customerId}.
     */
    public static Sale create(UserId sellerId,
                              List<SaleLineInput> lineRequests,
                              CustomerId customerId,
                              Money amountPaid) {
        return create(sellerId, lineRequests, customerId, amountPaid, LocalDateTime.now());
    }

    public static Sale create(UserId sellerId,
                              List<SaleLineInput> lineRequests,
                              CustomerId customerId,
                              Money amountPaid,
                              LocalDateTime occurredAt) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new InvalidSaleException(sellerId, "A sale must contain at least one line item");
        }
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");

        List<SaleLineItem> internalLines = lineRequests.stream()
                .map(request -> new SaleLineItem(request.productId(), request.quantity(), request.unitPrice()))
                .toList();

        Money totalAmount = sumOf(internalLines, "Sale.create invariant violated: lineRequests cannot be empty");

        // A missing initial payment amount means the sale is paid in full.
        Money effectiveAmountPaid = (amountPaid == null) ? totalAmount : amountPaid;

        if (effectiveAmountPaid.isNegative()) {
            throw new InvalidSaleException(sellerId, "The amount paid cannot be negative");
        }

        // A zero initial amount creates no payment entry.
        List<Payment> initialPayments = effectiveAmountPaid.isPositive()
                ? List.of(Payment.record(effectiveAmountPaid, sellerId, occurredAt))
                : List.of();

        return new Sale(
                SaleId.generate(),
                sellerId,
                occurredAt,
                totalAmount,
                internalLines,
                customerId,
                initialPayments);
    }

    /** Creates a fully paid cash sale without a customer. */
    public static Sale create(UserId sellerId, List<SaleLineInput> lineRequests) {
        return create(sellerId, lineRequests, null, null);
    }

    /** Reconstitutes a legacy cash sale as fully paid without a customer. */
    public static Sale rehydrate(SaleId saleId,
                                 UserId soldBy,
                                 LocalDateTime occurredAt,
                                 Money totalAmount,
                                 List<SaleLineDto> lines) {
        return rehydrate(saleId, soldBy, occurredAt, totalAmount, lines, null,
                List.of(Payment.record(totalAmount, soldBy, occurredAt)));
    }

    /** Reconstitutes a persisted sale without generating new IDs or timestamps. */
    public static Sale rehydrate(SaleId saleId,
                                 UserId soldBy,
                                 LocalDateTime occurredAt,
                                 Money totalAmount,
                                 List<SaleLineDto> lines,
                                 CustomerId customerId,
                                 List<Payment> payments) {
        Objects.requireNonNull(lines, "lines cannot be null");

        if (lines.isEmpty()) {
            throw new InvalidSaleException(soldBy, "A sale must contain at least one line item");
        }

        List<SaleLineItem> internalLines = lines.stream()
                .map(line -> new SaleLineItem(line.productId(), line.quantity(), line.unitPrice()))
                .toList();

        Money recalculatedTotal =
                sumOf(internalLines, "Sale.rehydrate invariant violated: lines cannot be empty");

        if (!recalculatedTotal.equals(totalAmount)) {
            throw new InvalidSaleException(soldBy, "Persisted sale total does not match sale lines total");
        }

        return new Sale(saleId, soldBy, occurredAt, totalAmount, internalLines, customerId, payments);
    }

    /**
     * Records a repayment. Throws {@link SaleAlreadySettledException} if nothing is due or {@link
     * PaymentExceedsAmountDueException} if the payment exceeds the balance.
     */
    public Payment recordPayment(Money amount, UserId receivedBy, LocalDateTime receivedAt) {
        Money amountDue = getAmountDue();

        if (!amountDue.isPositive()) {
            throw new SaleAlreadySettledException(saleId);
        }

        // Payment validates that its amount is strictly positive.
        Payment payment = Payment.record(amount, receivedBy, receivedAt);

        // Subtraction rejects mismatched currencies.
        if (amountDue.subtract(amount).isNegative()) {
            throw new PaymentExceedsAmountDueException(amount, amountDue);
        }

        payments.add(payment);
        return payment;
    }

    /**
     * Validate overpayment before requiring a debtor, since an overpayment would otherwise produce
     * a negative balance and bypass that check.
     */
    private static void requireConsistentPaymentTerms(UserId sellerId,
                                                      Money totalAmount,
                                                      Money collected,
                                                      CustomerId customerId) {
        Money amountDue = totalAmount.subtract(collected);

        if (amountDue.isNegative()) {
            throw new InvalidSaleException(sellerId, "The amount paid cannot exceed the sale total");
        }

        if (amountDue.isPositive() && customerId == null) {
            throw new CreditSaleRequiresCustomerException(amountDue);
        }
    }

    private static Money sumOfPayments(List<Payment> payments, Currency currency) {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(Money::add)
                .orElseGet(() -> Money.zero(currency));
    }

    private static Money sumOf(List<SaleLineItem> lines, String emptyMessage) {
        return lines.stream()
                .map(SaleLineItem::getLineTotal)
                .reduce(Money::add)
                .orElseThrow(() -> new IllegalStateException(emptyMessage));
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

    /** Total of all recorded payments. */
    public Money getAmountPaid() {
        return sumOfPayments(payments, totalAmount.getCurrency());
    }

    /** Payments ordered oldest first. */
    public List<Payment> getPayments() {
        return List.copyOf(payments);
    }

    /** Optional debtor; absent for a cash sale without a customer. */
    public Optional<CustomerId> getCustomerId() {
        return Optional.ofNullable(customerId);
    }

    /** Derived outstanding balance, zero when fully paid. */
    public Money getAmountDue() {
        return totalAmount.subtract(getAmountPaid());
    }

    /** Whether an outstanding balance remains. */
    public boolean isOnCredit() {
        return getAmountDue().isPositive();
    }

    public List<SaleLineDto> getLines() {
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
        return saleId.hashCode();
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
}
