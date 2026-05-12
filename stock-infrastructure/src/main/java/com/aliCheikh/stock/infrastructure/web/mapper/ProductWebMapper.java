package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.web.dto.MoneyResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfProductResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ProductResponse;

import java.util.ArrayList;
import java.util.List;

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

    public static PageOfProductResponse toPageResponse(
            List<Product> products,
            int page,
            int size,
            long totalElements
    ) {
        List<ProductResponse> content = products.stream()
                .map(ProductWebMapper::toResponse)
                .toList();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PageOfProductResponse(
                content,
                new PageMetaResponse(page, size, totalElements, totalPages)
        );
    }

}

