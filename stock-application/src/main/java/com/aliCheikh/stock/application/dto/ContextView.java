package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shop.ShopId;

import java.util.List;

public record ContextView(ShopId shopId, List<ContextLocationView> locations) {
}
