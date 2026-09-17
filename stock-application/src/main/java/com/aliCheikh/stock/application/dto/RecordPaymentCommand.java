package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.Objects;

/** Request to record a payment against a credit sale. */
public record RecordPaymentCommand(SaleId saleId, Money amount, UserId receivedBy) {

    public RecordPaymentCommand {
        Objects.requireNonNull(saleId, "saleId cannot be null");
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(receivedBy, "receivedBy cannot be null");
    }
}
