package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReceivingDistributionRequest(UUID locationId,
                                           @Positive int quantity) {
}
