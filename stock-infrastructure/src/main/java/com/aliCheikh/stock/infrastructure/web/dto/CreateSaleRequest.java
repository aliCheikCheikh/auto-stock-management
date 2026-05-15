package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateSaleRequest(@NotNull UUID sellerId,
                                UUID shopId,
                                @Valid @NotEmpty List<CreateSaleLine> lines) {
}
