package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateSaleRequest(@NotNull UUID sellerId,
                                UUID shopId,
                                @NotEmpty List<CreateSaleLine> lines) {
}
