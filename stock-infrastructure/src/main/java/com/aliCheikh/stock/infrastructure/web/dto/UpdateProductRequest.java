package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateProductRequest(@NotBlank String name,
                                   @NotNull @Valid MoneyRequest unitPrice,
                                   @PositiveOrZero int minimumGlobalThreshold) {
}
