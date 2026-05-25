package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.GetProductStockLevelsQuery;
import com.aliCheikh.stock.application.dto.ProductStockSummaryView;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.port.ProductStockQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetProductStockLevelsUseCaseTest {

    private ProductStockQueryPort productStockQueryPort;
    private GetProductStockLevelsUseCase getProductStockLevelsUseCase;

    @BeforeEach
    void setUp() {
        productStockQueryPort = mock(ProductStockQueryPort.class);
        getProductStockLevelsUseCase = new GetProductStockLevelsUseCase(productStockQueryPort);
    }

    @Test
    void should_delegate_product_stock_summary_to_query_port() {
        ProductId productId = ProductId.generate();
        ShopId shopId = ShopId.generate();
        LocationId locationId = LocationId.generate();

        GetProductStockLevelsQuery query = new GetProductStockLevelsQuery(
                productId,
                shopId
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

        ProductStockSummaryView summary = new ProductStockSummaryView(
                productId,
                "Oil Filter",
                7,
                10,
                true,
                List.of(stockLevel)
        );

        Optional<ProductStockSummaryView> expectedSummary = Optional.of(summary);

        when(productStockQueryPort.findProductStockSummary(query)).thenReturn(expectedSummary);

        Optional<ProductStockSummaryView> result = getProductStockLevelsUseCase.execute(query);

        assertThat(result).isEqualTo(expectedSummary);
        verify(productStockQueryPort).findProductStockSummary(query);
    }
}