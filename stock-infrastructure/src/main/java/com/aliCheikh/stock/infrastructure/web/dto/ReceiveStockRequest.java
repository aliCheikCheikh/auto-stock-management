package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record ReceiveStockRequest(String productReference,
                                  ProductInfoRequest newProductInfo,
                                  UUID shopId,
                                  UUID userId,
                                  List<ReceivingDistributionRequest> distributions) {
}
