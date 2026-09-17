package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record SaleView(SaleId saleId,
                       UserId sellerId,
                       /** Seller display name. */
                       String sellerName,
                       List<SaleLineDto> lines,
                       Money totalAmount,
                       LocalDateTime createdAt,
                       /**
                        * Balance derived from the payment ledger: zero for a settled sale,
                        * positive for an outstanding sale.
                        */
                       Money amountDue) {
    public SaleView {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines cannot be null"));
    }
}
