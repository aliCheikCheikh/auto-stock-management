package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;

public record SellProductResult(
        SaleId saleId,
        UserId sellerId,
        List<SaleLineDto> lines,
        Money totalAmount,
        LocalDateTime createdAt
) {
}
