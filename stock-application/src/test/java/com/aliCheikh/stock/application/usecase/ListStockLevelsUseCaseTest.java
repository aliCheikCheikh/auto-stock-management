package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.port.StockLevelQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListStockLevelsUseCaseTest {

    private StockLevelQueryPort stockLevelQueryPort;
    private ListStockLevelsUseCase listStockLevelsUseCase;

    @BeforeEach
    void setUp() {
        stockLevelQueryPort = mock(StockLevelQueryPort.class);
        listStockLevelsUseCase = new ListStockLevelsUseCase(stockLevelQueryPort);
    }

    @Test
    void should_delegate_stock_levels_listing_to_query_port() {
        ProductId productId = ProductId.generate();
        ShopId shopId = ShopId.generate();
        LocationId locationId = LocationId.generate();

        ListStockLevelsQuery query = new ListStockLevelsQuery(
                0,
                20,
                productId,
                shopId,
                locationId,
                true
        );

        StockLevelView stockLevel = new StockLevelView(
                productId,
                "Oil Filter",
                locationId,
                "Shop floor",
                LocationType.SHOP_FLOOR,
                shopId,
                7
        );

        PageResult<StockLevelView> expectedPage = new PageResult<>(
                List.of(stockLevel),
                0,
                20,
                1,
                1
        );

        when(stockLevelQueryPort.findByQuery(query)).thenReturn(expectedPage);

        PageResult<StockLevelView> result = listStockLevelsUseCase.execute(query);

        assertThat(result).isEqualTo(expectedPage);
        verify(stockLevelQueryPort).findByQuery(query);
    }
}