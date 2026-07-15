package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.UUID;

public record ContextLocationResponse(String type, UUID locationId, String label) {
}
