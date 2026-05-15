package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.TransferStockCommand;
import com.aliCheikh.stock.application.dto.TransferStockResult;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.web.dto.StockTransferAcknowledgementResponse;
import com.aliCheikh.stock.infrastructure.web.dto.TransferStockRequest;

public final class TransferStockWebMapper {

    private TransferStockWebMapper() {
    }

    public static TransferStockCommand toCommand(TransferStockRequest request) {
        return new TransferStockCommand(
                ProductId.of(request.productId()),
                LocationId.of(request.sourceLocationId()),
                LocationId.of(request.destinationLocationId()),
                request.quantity(),
                UserId.of(request.userId())
        );
    }

    public static StockTransferAcknowledgementResponse toResponse(TransferStockResult result) {
        return new StockTransferAcknowledgementResponse(
                result.movementId().getValue(),
                result.acceptedAt()
        );
    }
}
