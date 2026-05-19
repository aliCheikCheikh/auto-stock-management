package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.SellLineCommand;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleLine;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleRequest;
import com.aliCheikh.stock.infrastructure.web.dto.MoneyResponse;
import com.aliCheikh.stock.infrastructure.web.dto.SaleLineResponse;
import com.aliCheikh.stock.infrastructure.web.dto.SaleResponse;

public final class SaleWebMapper {

    private SaleWebMapper() {
    }

    public static SellProductCommand toCommand(CreateSaleRequest request) {
        return new SellProductCommand(
                UserId.of(request.sellerId()),
                ShopId.of(request.shopId()),
                request.lines().stream()
                        .map(SaleWebMapper::toLineCommand)
                        .toList()
        );
    }

    public static SaleResponse toResponse(SellProductResult result) {
        return new SaleResponse(
                result.saleId().getValue(),
                result.sellerId().getValue(),
                result.lines().stream()
                        .map(SaleWebMapper::toSaleLineResponse)
                        .toList(),
                moneyToResponse(result.totalAmount()),
                result.createdAt()
        );
    }

    public static SaleResponse toResponse(Sale sale) {
        return new SaleResponse(sale.getSaleId().getValue(),
                sale.getSoldBy().getValue(),
                sale.getLines()
                        .stream()
                        .map(line -> new SaleLineResponse(line.productId().getValue(),
                                line.quantity(),
                                moneyToResponse(line.unitPrice()),
                                moneyToResponse(line.lineTotal()))
                        ).toList(), moneyToResponse(sale.getTotalAmount()), sale.getOccurredAt());
    }

    private static SellLineCommand toLineCommand(CreateSaleLine line) {
        return new SellLineCommand(
                ProductId.of(line.productId()),
                line.quantity()
        );
    }

    private static SaleLineResponse toSaleLineResponse(SaleLineDto line) {
        return new SaleLineResponse(
                line.productId().getValue(),
                line.quantity(),
                moneyToResponse(line.unitPrice()),
                moneyToResponse(line.lineTotal())
        );
    }

    private static MoneyResponse moneyToResponse(Money money) {
        return new MoneyResponse(
                money.getAmount().toPlainString(),
                money.getCurrency().getCurrencyCode()
        );
    }
}