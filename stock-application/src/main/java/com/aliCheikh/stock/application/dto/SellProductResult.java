package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Sale result, including the remaining amount due. */
public record SellProductResult(
        SaleId saleId,
        UserId sellerId,
        List<SaleLineDto> lines,
        Money totalAmount,
        LocalDateTime createdAt,
        Optional<CustomerId> customerId,
        Money amountPaid,
        Money amountDue
) {

    /** Fully paid cash sale without a customer. */
    public SellProductResult(SaleId saleId,
                             UserId sellerId,
                             List<SaleLineDto> lines,
                             Money totalAmount,
                             LocalDateTime createdAt) {
        this(saleId, sellerId, lines, totalAmount, createdAt,
                Optional.empty(),
                totalAmount,
                totalAmount.subtract(totalAmount));
    }
}
