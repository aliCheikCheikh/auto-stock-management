package com.aliCheikh.stock.domain.exception.sale;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.shared.Money;

/** Raised when a sale leaves an outstanding balance without an identified customer. */
public class CreditSaleRequiresCustomerException extends DomainException {

    private final Money amountDue;

    public CreditSaleRequiresCustomerException(Money amountDue) {
        super("A sale leaving an outstanding balance requires a customer; amount due: " + amountDue);
        this.amountDue = amountDue;
    }

    /** Balance that would remain if the sale were accepted. */
    public Money getAmountDue() {
        return amountDue;
    }
}
