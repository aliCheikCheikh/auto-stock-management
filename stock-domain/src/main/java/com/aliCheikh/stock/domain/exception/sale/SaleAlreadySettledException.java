package com.aliCheikh.stock.domain.exception.sale;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.sale.SaleId;

/** Raised when a payment is attempted on an already settled sale. */
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
