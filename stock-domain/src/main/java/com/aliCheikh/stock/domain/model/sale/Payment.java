package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Un encaissement rattaché à une vente : l'acompte du jour de la vente, ou un remboursement ultérieur.
 *
 * <p>Entité interne à l'agrégat {@link Sale} : elle n'a pas d'existence hors de la vente qu'elle
 * solde, mais elle possède une <b>identité propre</b>. Deux encaissements du même montant, le même
 * jour, par le même vendeur restent deux faits distincts — les confondre reviendrait à perdre un
 * versement. C'est aussi ce qui permet de les réécrire sans les recréer.</p>
 *
 * <p>Immuable : on n'annule pas un encaissement, on en enregistre un autre.</p>
 *
 * <p>L'auteur et la date sont conservés parce qu'il s'agit d'argent : savoir qui a encaissé quoi et
 * quand est une exigence de traçabilité, pas un confort.</p>
 */
public final class Payment {

    private final PaymentId paymentId;
    private final Money amount;
    private final UserId receivedBy;
    private final LocalDateTime receivedAt;

    private Payment(PaymentId paymentId, Money amount, UserId receivedBy, LocalDateTime receivedAt) {
        this.paymentId = Objects.requireNonNull(paymentId, "paymentId cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.receivedBy = Objects.requireNonNull(receivedBy, "receivedBy cannot be null");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt cannot be null");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("A payment amount must be strictly positive");
        }
    }

    /** Nouvel encaissement, dont l'identité est générée. */
    public static Payment record(Money amount, UserId receivedBy, LocalDateTime receivedAt) {
        return new Payment(PaymentId.generate(), amount, receivedBy, receivedAt);
    }

    /** Encaissement relu depuis la persistance, dont l'identité est conservée. */
    public static Payment rehydrate(PaymentId paymentId,
                                    Money amount,
                                    UserId receivedBy,
                                    LocalDateTime receivedAt) {
        return new Payment(paymentId, amount, receivedBy, receivedAt);
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public Money getAmount() {
        return amount;
    }

    public UserId getReceivedBy() {
        return receivedBy;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Payment other = (Payment) o;
        return paymentId.equals(other.paymentId);
    }

    @Override
    public int hashCode() {
        return paymentId.hashCode();
    }

    @Override
    public String toString() {
        return amount + " le " + receivedAt;
    }
}
