package com.aliCheikh.stock.domain.exception.sale;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.sale.SaleId;

public class SaleNotFoundException extends DomainException {

    private final SaleId saleId;

    public SaleNotFoundException(SaleId saleId) {
        super("Sale not found for saleId: " + saleId);
        this.saleId = saleId;
    }

    public SaleId getSaleId() {
        return saleId;
    }
}
