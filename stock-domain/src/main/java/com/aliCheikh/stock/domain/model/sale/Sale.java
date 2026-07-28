package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.exception.sale.CreditSaleRequiresCustomerException;
import com.aliCheikh.stock.domain.exception.sale.InvalidSaleException;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Vente réalisée par un vendeur : un fait daté, immuable une fois enregistré.
 *
 * <p><b>Vente au comptant ou à crédit.</b> Le montant encaissé au moment de la vente
 * ({@code amountPaid}) peut couvrir la totalité, une partie seulement, ou rien du tout. Ce qui
 * reste à payer — la <i>créance</i> — n'est jamais stocké : il se déduit du total et du montant
 * payé (voir {@link #getAmountDue()}). Une donnée dérivable ne se duplique pas, sous peine de se
 * désynchroniser.</p>
 *
 * <p><b>Règle métier centrale :</b> si un solde reste dû, la vente <b>doit</b> être rattachée à un
 * client. On ne fait pas crédit à un anonyme — sans client, la créance serait irrécouvrable. À
 * l'inverse, une vente intégralement payée n'exige aucun client.</p>
 */
public class Sale {

    private final SaleId saleId;
    private final UserId soldBy;
    private final LocalDateTime occurredAt;
    private final Money totalAmount;
    private final List<SaleLineItem> lines;

    /** Client débiteur. {@code null} pour une vente au comptant, obligatoire dès qu'un solde reste dû. */
    private final CustomerId customerId;

    /** Montant encaissé au moment de la vente. Jamais nul : une vente au comptant vaut le total. */
    private final Money amountPaid;

    /**
     * Porte les invariants de l'agrégat : tout chemin de construction, présent ou futur,
     * passe obligatoirement par ici.
     */
    private Sale(SaleId saleId,
                 UserId soldBy,
                 LocalDateTime occurredAt,
                 Money totalAmount,
                 List<SaleLineItem> lines,
                 CustomerId customerId,
                 Money amountPaid) {
        Objects.requireNonNull(saleId, "saleId cannot be null");
        Objects.requireNonNull(soldBy, "soldBy cannot be null");
        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        Objects.requireNonNull(totalAmount, "totalAmount cannot be null");
        Objects.requireNonNull(lines, "lines cannot be null");
        Objects.requireNonNull(amountPaid, "amountPaid cannot be null");

        requireConsistentPaymentTerms(soldBy, totalAmount, amountPaid, customerId);

        this.saleId = saleId;
        this.soldBy = soldBy;
        this.occurredAt = occurredAt;
        this.totalAmount = totalAmount;
        this.lines = List.copyOf(lines);
        this.customerId = customerId;
        this.amountPaid = amountPaid;
    }

    /**
     * Enregistre une vente, éventuellement à crédit.
     *
     * @param sellerId     le vendeur, jamais nul
     * @param lineRequests les lignes vendues, au moins une
     * @param customerId   le client débiteur ; peut être {@code null} si la vente est intégralement
     *                     payée, obligatoire sinon
     * @param amountPaid   le montant encaissé ; {@code null} signifie « payé en entier »
     */
    public static Sale create(UserId sellerId,
                              List<SaleLineInput> lineRequests,
                              CustomerId customerId,
                              Money amountPaid) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new InvalidSaleException(sellerId, "A sale must contain at least one line item");
        }

        List<SaleLineItem> internalLines = lineRequests.stream()
                .map(request -> new SaleLineItem(request.productId(), request.quantity(), request.unitPrice()))
                .toList();

        Money totalAmount = sumOf(internalLines, "Sale.create invariant violated: lineRequests cannot be empty");

        // Absence d'acompte = vente au comptant : le client repart sans rien devoir.
        Money effectiveAmountPaid = (amountPaid == null) ? totalAmount : amountPaid;

        return new Sale(
                SaleId.generate(),
                sellerId,
                LocalDateTime.now(),
                totalAmount,
                internalLines,
                customerId,
                effectiveAmountPaid);
    }

    /**
     * Enregistre une vente au comptant, intégralement payée et sans client rattaché.
     *
     * <p>Surcharge de commodité : elle préserve les appelants antérieurs à la gestion des
     * créances, dont le comportement reste strictement inchangé.</p>
     */
    public static Sale create(UserId sellerId, List<SaleLineInput> lineRequests) {
        return create(sellerId, lineRequests, null, null);
    }

    /**
     * Reconstruit une vente au comptant déjà persistée.
     *
     * <p>Surcharge de commodité pour les données antérieures à la gestion des créances : elles ont
     * toutes été intégralement payées et n'ont pas de client rattaché.</p>
     */
    public static Sale rehydrate(SaleId saleId,
                                 UserId soldBy,
                                 LocalDateTime occurredAt,
                                 Money totalAmount,
                                 List<SaleLineDto> lines) {
        return rehydrate(saleId, soldBy, occurredAt, totalAmount, lines, null, totalAmount);
    }

    /** Reconstruit une vente déjà persistée, sans rejouer la génération d'identifiant ni l'horodatage. */
    public static Sale rehydrate(SaleId saleId,
                                 UserId soldBy,
                                 LocalDateTime occurredAt,
                                 Money totalAmount,
                                 List<SaleLineDto> lines,
                                 CustomerId customerId,
                                 Money amountPaid) {
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

        return new Sale(saleId, soldBy, occurredAt, totalAmount, internalLines, customerId, amountPaid);
    }

    /**
     * Vérifie la cohérence entre le total, le montant encaissé et la présence d'un client.
     *
     * <p>L'ordre des contrôles est structurant : tant qu'on n'a pas écarté un encaissement
     * supérieur au total, le solde dû peut être négatif — auquel cas il n'est pas « strictement
     * positif » et le contrôle du client serait silencieusement sauté.</p>
     */
    private static void requireConsistentPaymentTerms(UserId sellerId,
                                                      Money totalAmount,
                                                      Money amountPaid,
                                                      CustomerId customerId) {
        if (amountPaid.isNegative()) {
            throw new InvalidSaleException(sellerId, "The amount paid cannot be negative");
        }

        Money amountDue = totalAmount.subtract(amountPaid);

        if (amountDue.isNegative()) {
            throw new InvalidSaleException(sellerId, "The amount paid cannot exceed the sale total");
        }

        if (amountDue.isPositive() && customerId == null) {
            throw new CreditSaleRequiresCustomerException(amountDue);
        }
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

    /** Le montant encaissé au moment de la vente. */
    public Money getAmountPaid() {
        return amountPaid;
    }

    /** Le client débiteur, s'il y en a un. Vide pour une vente au comptant. */
    public Optional<CustomerId> getCustomerId() {
        return Optional.ofNullable(customerId);
    }

    /** Le solde restant dû, calculé à la demande. Vaut zéro pour une vente intégralement payée. */
    public Money getAmountDue() {
        return totalAmount.subtract(amountPaid);
    }

    /** {@code true} si cette vente porte encore une créance. */
    public boolean isOnCredit() {
        return getAmountDue().isPositive();
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
