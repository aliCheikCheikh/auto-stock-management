package com.aliCheikh.stock.domain.exception.sale;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.sale.SaleId;

/** Levée quand on tente d'encaisser sur une vente qui ne doit plus rien. */
public class SaleAlreadySettledException extends DomainException {

    private final SaleId saleId;

    public SaleAlreadySettledException(SaleId saleId) {
        super("Sale " + saleId + " is already settled: nothing left to collect");
        this.saleId = saleId;
    }

    public SaleId getSaleId() {
        return saleId;
    }
}
