package com.aliCheikh.stock.domain.exception.sale;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.shared.Money;

/**
 * Levée quand un encaissement dépasse ce que le client reste devoir.
 *
 * <p>Le magasin ne conserve pas de trop-perçu : le vendeur rend la monnaie, il n'enregistre que ce
 * qui solde la dette.</p>
 */
public class PaymentExceedsAmountDueException extends DomainException {

    private final Money attemptedAmount;
    private final Money amountDue;

    public PaymentExceedsAmountDueException(Money attemptedAmount, Money amountDue) {
        super("Payment of " + attemptedAmount + " exceeds the outstanding balance of " + amountDue);
        this.attemptedAmount = attemptedAmount;
        this.amountDue = amountDue;
    }

    public Money getAttemptedAmount() {
        return attemptedAmount;
    }

    public Money getAmountDue() {
        return amountDue;
    }
}
