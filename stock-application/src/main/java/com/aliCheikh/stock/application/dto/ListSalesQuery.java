package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record ListSalesQuery(int page,
                             int size,
                             List<String> sort,
                             UserId sellerId,
                             ShopId shopId,
                             LocalDateTime from,
                             LocalDateTime to) {
    public ListSalesQuery {
        sort = List.copyOf(Objects.requireNonNull(sort, "sort cannot be null"));

        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to zero");
        }

        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }
    }
}
