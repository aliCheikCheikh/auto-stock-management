package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.ProductStockSummaryView;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.dto.UpdateProductCommand;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.stock.StockLevel;
import com.aliCheikh.stock.infrastructure.web.dto.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public final class ProductWebMapper {
    private ProductWebMapper() {

    }


    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getProductId().getValue(),
                product.getName(),
                product.getReference(),
                product.getCategoryId().getValue(),
                moneyToResponse(product.getUnitPrice()),
                product.getMinimumGlobalThreshold());

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

    public static ProductStockSummaryResponse toProductStockSummaryResponse(ProductStockSummaryView view) {
        return new ProductStockSummaryResponse(view.productId().getValue(),
                view.productName(),
                view.globalQuantity(),
                view.minimumGlobalThreshold(),
                view.belowGlobalThreshold(),
                view.byLocation().stream().map(ProductWebMapper::toStockLevelResponse).toList());
    }

    private static StockLevelResponse toStockLevelResponse(StockLevelView level) {
        return new StockLevelResponse(level.productId().getValue(),
                level.productName(),
                level.locationId().getValue(),
                level.locationName(),
                level.locationType(),
                level.shopId().getValue(),
                level.quantity());
    }

    public static UpdateProductCommand toCommand(UUID productId, UpdateProductRequest request) {
        return new UpdateProductCommand(
                ProductId.of(productId),
                request.name(),
                toMoney(request.unitPrice()),
                request.minimumGlobalThreshold()
        );
    }

    private static Money toMoney(MoneyRequest unitPrice) {
        return Money.create(
                new BigDecimal(unitPrice.amount()),
                Currency.getInstance(unitPrice.currency())
        );
    }

}

