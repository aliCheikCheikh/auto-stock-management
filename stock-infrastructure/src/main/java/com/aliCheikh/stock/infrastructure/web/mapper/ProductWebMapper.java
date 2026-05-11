package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.web.dto.MoneyResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ProductResponse;

public final class ProductWebMapper {
    private ProductWebMapper() {

    }


    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getProductId().getValue(),
                product.getName(),
                product.getReference(),
                product.getCategoryId().getValue(), moneyToResponse(product.getUnitPrice()), product.getMinimumGlobalThreshold());

    }


    private static MoneyResponse moneyToResponse(Money money) {
        return new MoneyResponse(money.getAmount().toPlainString()
                , money.getCurrency().getCurrencyCode());
    }
}
