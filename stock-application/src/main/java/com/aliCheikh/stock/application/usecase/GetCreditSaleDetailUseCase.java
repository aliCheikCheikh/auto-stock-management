package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;
import com.aliCheikh.stock.application.port.CreditSaleDetailQueryPort;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.model.sale.SaleId;

import java.util.Objects;

/** Reads a credit sale. A cash sale is treated as absent from the debt view. */
public class GetCreditSaleDetailUseCase {

    private final CreditSaleDetailQueryPort creditSaleDetailQueryPort;

    public GetCreditSaleDetailUseCase(CreditSaleDetailQueryPort creditSaleDetailQueryPort) {
        this.creditSaleDetailQueryPort = Objects.requireNonNull(
                creditSaleDetailQueryPort, "creditSaleDetailQueryPort cannot be null");
    }

    public CreditSaleDetailView detailOf(SaleId saleId) {
        Objects.requireNonNull(saleId, "saleId cannot be null");

        return creditSaleDetailQueryPort.findCreditSaleDetail(saleId.getValue())
                .orElseThrow(() -> new SaleNotFoundException(saleId));
    }
}
