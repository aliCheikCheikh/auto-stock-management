package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record ReceivingDistributionRequest(UUID locationId,
                                           int quantity) {
}
