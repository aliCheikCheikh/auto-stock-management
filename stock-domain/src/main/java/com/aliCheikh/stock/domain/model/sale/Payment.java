package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Un encaissement rattaché à une vente : l'acompte du jour de la vente, ou un remboursement ultérieur.
 *
 * <p>Value Object appartenant à l'agrégat {@link Sale} : un paiement n'a pas d'existence propre en
 * dehors de la vente qu'il solde. Il est immuable — on n'annule pas un encaissement, on en
 * enregistre un autre.</p>
 *
 * <p>L'auteur et la date sont conservés parce qu'il s'agit d'argent : savoir qui a encaissé quoi et
 * quand est une exigence de traçabilité, pas un confort.</p>
 */
public final class Payment {

    private final Money amount;
    private final UserId receivedBy;
    private final LocalDateTime receivedAt;

    private Payment(Money amount, UserId receivedBy, LocalDateTime receivedAt) {
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.receivedBy = Objects.requireNonNull(receivedBy, "receivedBy cannot be null");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt cannot be null");

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("A payment amount must be strictly positive");
        }
    }

    public static Payment of(Money amount, UserId receivedBy, LocalDateTime receivedAt) {
        return new Payment(amount, receivedBy, receivedAt);
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
        return amount.equals(other.amount)
                && receivedBy.equals(other.receivedBy)
                && receivedAt.equals(other.receivedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, receivedBy, receivedAt);
    }

    @Override
    public String toString() {
        return amount + " le " + receivedAt;
    }
}
