package com.aliCheikh.stock.domain.exception.sale;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.shared.Money;

/**
 * Levée quand une vente laisse un solde dû sans être rattachée à un client.
 *
 * <p>Sans client identifié, la créance serait irrécouvrable : on ne fait pas crédit à un anonyme.</p>
 */
public class CreditSaleRequiresCustomerException extends DomainException {

    private final Money amountDue;

    public CreditSaleRequiresCustomerException(Money amountDue) {
        super("A sale leaving an outstanding balance requires a customer; amount due: " + amountDue);
        this.amountDue = amountDue;
    }

    /** Le solde qui serait resté dû si la vente avait été acceptée. */
    public Money getAmountDue() {
        return amountDue;
    }
}
