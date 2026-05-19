package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.application.port.ListSalesQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ListSalesUseCaseTest {

    private ListSalesUseCase listSalesUseCase;
    private ListSalesQueryPort listSalesQueryPort;

    @BeforeEach
    void setUp() {
        listSalesQueryPort = mock(ListSalesQueryPort.class);
        listSalesUseCase = new ListSalesUseCase(listSalesQueryPort);
    }

    @Test
    void should_delegate_sales_listing_to_query_port() {
        UserId sellerId = UserId.generate();
        ShopId shopId = ShopId.generate();
        ProductId productId = ProductId.generate();
        Money unitPrice = Money.create(new BigDecimal("15.00"), Currency.getInstance("EUR"));

        SaleLineDto singleLine = new SaleLineDto(
                productId,
                4,
                unitPrice,
                unitPrice.multiply(4)
        );

        ListSalesQuery query = new ListSalesQuery(
                0,
                20,
                List.of("createdAt,desc"),
                sellerId,
                shopId,
                LocalDateTime.of(2026, 5, 19, 18, 30),
                LocalDateTime.of(2026, 5, 20, 23, 59)
        );

        SaleView saleView = new SaleView(
                SaleId.generate(),
                sellerId,
                List.of(singleLine),
                unitPrice.multiply(4),
                LocalDateTime.of(2026, 5, 20, 12, 30)
        );

        PageResult<SaleView> expectedPage = new PageResult<>(
                List.of(saleView),
                0,
                20,
                1,
                1
        );

        when(listSalesQueryPort.findByQuery(query)).thenReturn(expectedPage);

        PageResult<SaleView> result = listSalesUseCase.execute(query);

        assertThat(result).isEqualTo(expectedPage);
        verify(listSalesQueryPort).findByQuery(query);
    }
}