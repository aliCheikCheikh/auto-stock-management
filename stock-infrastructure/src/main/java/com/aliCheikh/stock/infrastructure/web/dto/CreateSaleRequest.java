package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CreateSaleRequest(UUID shopId,
                                @Valid @NotEmpty List<CreateSaleLine> lines) {
}
