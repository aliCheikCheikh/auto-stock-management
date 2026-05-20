package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;

public record StockLevelView(ProductId productId,
                             String productName,
                             LocationId locationId,
                             String locationName,
                             LocationType locationType,
                             ShopId shopId,
                             int quantity) {
}
