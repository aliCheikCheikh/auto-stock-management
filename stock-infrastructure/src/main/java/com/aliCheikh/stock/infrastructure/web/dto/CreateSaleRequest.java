package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record CreateSaleRequest(UUID sellerId,
                                UUID shopId,
                                List<CreateSaleLine> lines) {
}
