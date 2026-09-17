package com.aliCheikh.stock.domain.exception.sale;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.shared.Money;

/** Raised when a payment exceeds the outstanding balance; overpayments are not recorded. */
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
