package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MoneyRequest(@NotBlank String amount,
                           @NotBlank String currency) {
}
