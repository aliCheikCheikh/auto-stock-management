package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.ProductInfo;
import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.dto.ReceiveStockResult;
import com.aliCheikh.stock.application.dto.TargetLocation;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.web.dto.ProductInfoRequest;
import com.aliCheikh.stock.infrastructure.web.dto.ReceivingDistributionRequest;
import com.aliCheikh.stock.infrastructure.web.dto.ReceiveStockRequest;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptAcknowledgementResponse;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public final class ReceiveStockWebMapper {

    private ReceiveStockWebMapper() {
    }

    public static ReceiveStockCommand toCommand(ReceiveStockRequest request) {
        return new ReceiveStockCommand(
                request.productReference(),
                toProductInfo(request.newProductInfo()),
                ShopId.of(request.shopId()),
                UserId.of(request.userId()),
                toTargetLocations(request.distributions())
        );
    }

    public static StockReceiptAcknowledgementResponse toResponse(ReceiveStockResult result) {
        return new StockReceiptAcknowledgementResponse(
                result.productId().getValue(),
                result.totalReceived(),
                result.acceptedAt(),
                toMovementIds(result)
        );
    }

    private static ProductInfo toProductInfo(ProductInfoRequest productInfoRequest) {
        if (productInfoRequest == null) {
            return null;
        }

        return new ProductInfo(
                productInfoRequest.name(),
                productInfoRequest.reference(),
                CategoryId.of(productInfoRequest.categoryId()),
                toMoney(productInfoRequest),
                productInfoRequest.minimumGlobalThreshold()
        );
    }

    private static List<TargetLocation> toTargetLocations(List<ReceivingDistributionRequest> distributions) {
        return distributions.stream()
                .map(distribution -> new TargetLocation(
                        LocationId.of(distribution.locationId()),
                        distribution.quantity()
                ))
                .toList();
    }

    private static Money toMoney(ProductInfoRequest productInfoRequest) {
        return Money.create(
                new BigDecimal(productInfoRequest.unitPrice().amount()),
                Currency.getInstance(productInfoRequest.unitPrice().currency())
        );
    }

    private static List<UUID> toMovementIds(ReceiveStockResult result) {
        return result.movementIds().stream()
                .map(movementId -> movementId.getValue())
                .toList();
    }
}
