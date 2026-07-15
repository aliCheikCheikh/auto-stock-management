package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ProductSearchView;
import com.aliCheikh.stock.application.port.ProductSearchQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SearchProductsUseCaseTest {

    private static final int MAX_RESULTS = 10;

    private SearchProductsUseCase searchProductsUseCase;
    private ProductSearchQueryPort productSearchQueryPort;

    @BeforeEach
    void setUp() {
        productSearchQueryPort = mock(ProductSearchQueryPort.class);
        searchProductsUseCase = new SearchProductsUseCase(productSearchQueryPort);
    }

    @Test
    void should_return_empty_list_and_not_touch_the_port_when_keyword_is_null() {
        List<ProductSearchView> result = searchProductsUseCase.findProductsByKeyword(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(productSearchQueryPort);
    }

    @Test
    void should_return_empty_list_and_not_touch_the_port_when_keyword_is_too_short() {
        List<ProductSearchView> result = searchProductsUseCase.findProductsByKeyword("a");

        assertThat(result).isEmpty();
        verifyNoInteractions(productSearchQueryPort);
    }

    @Test
    void should_return_empty_list_when_keyword_is_only_whitespace() {
        List<ProductSearchView> result = searchProductsUseCase.findProductsByKeyword("   ");

        assertThat(result).isEmpty();
        verifyNoInteractions(productSearchQueryPort);
    }

    @Test
    void should_trim_keyword_and_delegate_to_port_with_max_results() {
        searchProductsUseCase.findProductsByKeyword("  filtre  ");

        verify(productSearchQueryPort).findProductsByKeyword("filtre", MAX_RESULTS);
    }

    @Test
    void should_return_the_results_provided_by_the_port() {
        ProductSearchView view = new ProductSearchView(
                ProductId.generate(),
                "Filtre à huile",
                "FIL-001",
                Money.create(new BigDecimal("2500.00"), Currency.getInstance("XAF"))
        );
        when(productSearchQueryPort.findProductsByKeyword("filtre", MAX_RESULTS))
                .thenReturn(List.of(view));

        List<ProductSearchView> result = searchProductsUseCase.findProductsByKeyword("filtre");

        assertThat(result).containsExactly(view);
        verify(productSearchQueryPort).findProductsByKeyword("filtre", MAX_RESULTS);
    }
}
