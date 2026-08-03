package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.ListStockMovementsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockMovementView;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.infrastructure.web.dto.MoneyResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfStockMovementResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockMovementResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class StockMovementWebMapper {

    private static final List<String> DEFAULT_SORT = List.of("executedAt,desc");

    private StockMovementWebMapper() {
    }

    public static ListStockMovementsQuery toQuery(
            int page,
            int size,
            List<String> sort,
            UUID productId,
            UUID locationId,
            MovementType type,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return new ListStockMovementsQuery(
                page,
                size,
                normalizeSort(sort),
                productId == null ? null : ProductId.of(productId),
                locationId == null ? null : LocationId.of(locationId),
                type,
                from,
                to
        );
    }

    public static PageOfStockMovementResponse toPageResponse(PageResult<StockMovementView> page) {
        return new PageOfStockMovementResponse(
                page.content().stream()
                        .map(StockMovementWebMapper::toResponse)
                        .toList(),
                new PageMetaResponse(
                        page.page(),
                        page.size(),
                        page.totalElements(),
                        page.totalPages()
                )
        );
    }

    private static StockMovementResponse toResponse(StockMovementView movement) {
        return new StockMovementResponse(
                movement.movementId().getValue(),
                movement.productId().getValue(),
                movement.locationId().getValue(),
                movement.destinationLocationId() == null ? null : movement.destinationLocationId().getValue(),
                movement.type(),
                movement.quantity(),
                movement.executedBy().getValue(),
                movement.executedByName(),
                movement.executedAt(),
                movement.saleId() == null ? null : movement.saleId().getValue(),
                movement.operationId().getValue(),
                MoneyResponse.from(movement.saleAmountDue())
        );
    }

    private static List<String> normalizeSort(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return DEFAULT_SORT;
        }

        return sort;
    }
}
