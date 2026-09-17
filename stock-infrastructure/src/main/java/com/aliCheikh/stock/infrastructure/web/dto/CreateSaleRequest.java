package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Sale request. Missing customer and payment amount imply a cash sale. The domain requires a
 * customer when the amount paid is below the total.
 */
public record CreateSaleRequest(UUID shopId,
                                @Valid @NotEmpty List<CreateSaleLine> lines,
                                UUID customerId,
                                @PositiveOrZero BigDecimal amountPaid) {
}
