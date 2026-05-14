package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReceiveStockRequest(@NotBlank String productReference,
                                  ProductInfoRequest newProductInfo,
                                  UUID shopId,
                                  UUID userId,
                                  @NotEmpty List<ReceivingDistributionRequest> distributions) {
}
