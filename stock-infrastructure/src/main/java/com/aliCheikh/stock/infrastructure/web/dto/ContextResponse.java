package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

public record ContextResponse(UUID shopId, List<ContextLocationResponse> locations) {
}
