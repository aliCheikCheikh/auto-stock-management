package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;

public record ContextLocationView(LocationType type, LocationId locationId, String label) {
}
