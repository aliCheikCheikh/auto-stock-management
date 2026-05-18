package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ListStockMovementsQuery(
        int page,
        int size,
        List<String> sort,
        ProductId productId,
        LocationId locationId,
        MovementType type,
        LocalDateTime from,
        LocalDateTime to
) {
    public ListStockMovementsQuery {
        sort = List.copyOf(Objects.requireNonNull(sort, "sort cannot be null"));

        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }

        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }
    }
}
