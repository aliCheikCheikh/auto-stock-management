package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfStockLevelResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockLevelResponse;

import java.util.UUID;

public class StockLevelWebMapper {

    public static ListStockLevelsQuery toQuery(int page,
                                               int size,
                                               UUID productId,
                                               UUID shopId,
                                               UUID locationId,
                                               Boolean belowThreshold) {
        return new ListStockLevelsQuery(page,
                size,
                productId == null ? null : ProductId.of(productId),
                shopId == null ? null : ShopId.of(shopId),
                locationId == null ? null : LocationId.of(locationId),
                belowThreshold);
    }

    public static PageOfStockLevelResponse toPageResponse(PageResult<StockLevelView> page) {
        return new PageOfStockLevelResponse(page.content().stream().map(StockLevelWebMapper::toResponse).toList(),
                new PageMetaResponse(page.page(),
                        page.size(),
                        page.totalElements(),
                        page.totalPages()));
    }

    private static StockLevelResponse toResponse(StockLevelView stockLevelView) {
        return new StockLevelResponse(stockLevelView.productId().getValue(),
                stockLevelView.productName(),
                stockLevelView.locationId().getValue(),
                stockLevelView.locationName(),
                stockLevelView.locationType(),
                stockLevelView.shopId().getValue(),
                stockLevelView.quantity());
    }
}
