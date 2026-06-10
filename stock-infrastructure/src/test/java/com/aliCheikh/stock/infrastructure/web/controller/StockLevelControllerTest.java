package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.usecase.ListStockLevelsUseCase;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockLevelController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockLevelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListStockLevelsUseCase listStockLevelsUseCase;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    private UUID productId;
    private UUID locationId;
    private UUID shopId;


    @BeforeEach
    void setUp() {
        productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        locationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        shopId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    }

    @Test
    void should_return_stock_level_page() throws Exception {

        StockLevelView stockLevelView = new StockLevelView(ProductId.of(productId),
                "Product 1",
                LocationId.of(locationId),
                "Magasin Principal",
                LocationType.SHOP_FLOOR,
                ShopId.of(shopId),
                4);

        given(listStockLevelsUseCase.execute(any(ListStockLevelsQuery.class))).willReturn(new PageResult<>(
                List.of(stockLevelView),
                0,
                20,
                1,
                1
        ));

        mockMvc.perform(get("/api/v1/stock-levels"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.content[0].productName").value("Product 1"))
                .andExpect(jsonPath("$.content[0].locationId").value(locationId.toString()))
                .andExpect(jsonPath("$.content[0].locationName").value("Magasin Principal"))
                .andExpect(jsonPath("$.content[0].locationType").value("SHOP_FLOOR"))
                .andExpect(jsonPath("$.content[0].shopId").value(shopId.toString()))
                .andExpect(jsonPath("$.content[0].quantity").value(4))
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));


        ArgumentCaptor<ListStockLevelsQuery> queryCaptor = ArgumentCaptor.forClass(ListStockLevelsQuery.class);
        verify(listStockLevelsUseCase).execute(queryCaptor.capture());
        ListStockLevelsQuery query = queryCaptor.getValue();

        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.productId()).isNull();
        assertThat(query.shopId()).isNull();
        assertThat(query.locationId()).isNull();
        assertThat(query.belowThreshold()).isFalse();


    }

    @Test
    void should_pass_query_parameters_to_stock_levels_use_case() throws Exception {
        given(listStockLevelsUseCase.execute(any(ListStockLevelsQuery.class)))
                .willReturn(new PageResult<>(List.of(), 1, 10, 0, 0));
        mockMvc.perform(get("/api/v1/stock-levels")
                        .param("page", "1")
                        .param("size", "10")
                        .param("productId", productId.toString())
                        .param("shopId", shopId.toString())
                        .param("locationId", locationId.toString())
                        .param("belowThreshold", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.page").value(1))
                .andExpect(jsonPath("$.page.size").value(10));

        ArgumentCaptor<ListStockLevelsQuery> queryCaptor =
                ArgumentCaptor.forClass(ListStockLevelsQuery.class);

        verify(listStockLevelsUseCase).execute(queryCaptor.capture());

        ListStockLevelsQuery query = queryCaptor.getValue();

        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(10);
        assertThat(query.productId()).isEqualTo(ProductId.of(productId));
        assertThat(query.shopId()).isEqualTo(ShopId.of(shopId));
        assertThat(query.locationId()).isEqualTo(LocationId.of(locationId));
        assertThat(query.belowThreshold()).isTrue();
    }

    @Test
    void should_return_400_when_stock_levels_page_is_negative() throws Exception {
        mockMvc.perform(get("/api/v1/stock-levels")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(listStockLevelsUseCase);
    }

    @Test
    void should_return_400_when_stock_levels_page_size_is_too_large() throws Exception {
        mockMvc.perform(get("/api/v1/stock-levels")
                        .param("size", "201"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(listStockLevelsUseCase);
    }
}
