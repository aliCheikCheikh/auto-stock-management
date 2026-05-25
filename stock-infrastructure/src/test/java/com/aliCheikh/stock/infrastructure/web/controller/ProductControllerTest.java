package com.aliCheikh.stock.infrastructure.web.controller;


import com.aliCheikh.stock.application.dto.GetProductStockLevelsQuery;
import com.aliCheikh.stock.application.dto.ProductStockSummaryView;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.usecase.GetProductStockLevelsUseCase;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ProductController.class)
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private GetProductStockLevelsUseCase getProductStockLevelsUseCase;

    private UUID productId;
    private UUID shopId;
    private UUID shopFloorId;
    private UUID backstockId;

    @BeforeEach
    void setUp() {
        productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        shopId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        shopFloorId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        backstockId = UUID.fromString("44444444-4444-4444-4444-444444444444");
    }

    @Test
    void should_return_problem_detail_when_product_does_not_exist() throws Exception {
        when(productRepository.findById(ProductId.of(productId)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://api.stock.example.com/errors/product-not-found"))
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString(productId.toString())))
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void should_return_200_when_product_exists() throws Exception {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID categoryId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Product product = new Product(
                ProductId.of(productId),
                "Plaquettes de frein",
                "BRK-PAD-001",
                CategoryId.of(categoryId),
                10,
                Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"))
        );

        when(productRepository.findById(ProductId.of(productId))).thenReturn(Optional.of(product));
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Plaquettes de frein"))
                .andExpect(jsonPath("$.reference").value("BRK-PAD-001"))
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.minimumGlobalThreshold").value(10))
                .andExpect(jsonPath("$.unitPrice.amount").value("45.90"))
                .andExpect(jsonPath("$.unitPrice.currency").value("EUR"));

    }

    @Test
    void should_return_400_when_product_id_is_not_uuid() throws Exception {
        mockMvc.perform(get("/api/v1/products/not-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(productRepository);
    }

    @Test
    void should_return_empty_page_when_no_products() throws Exception {
        given(productRepository.findAll(0, 20)).willReturn(List.of());
        given(productRepository.count()).willReturn(0L);
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.page.totalPages").value(0));
    }


    @Test
    void should_return_first_page_of_20_when_25_products_exist() throws Exception {
        List<Product> twentyProducts = IntStream.rangeClosed(1, 20).mapToObj(this::sampleProduct).toList();
        given(productRepository.findAll(0, 20)).willReturn(twentyProducts);
        given(productRepository.count()).willReturn(25L);
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(25))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void should_apply_custom_page_and_size() throws Exception {
        List<Product> tenProducts = IntStream.rangeClosed(1, 10).mapToObj(this::sampleProduct).toList();
        given(productRepository.findAll(1, 10)).willReturn(tenProducts);
        given(productRepository.count()).willReturn(25L);
        mockMvc.perform(get("/api/v1/products").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.page.page").value(1))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(25))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    void should_return_an_empty_content_when_page_is_out_of_range() throws Exception {
        given(productRepository.findAll(99, 20)).willReturn(List.of());
        given(productRepository.count()).willReturn(25L);
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "99")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.page").value(99))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(25))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void should_return_400_when_page_is_negative() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(productRepository);
    }

    @Test
    void should_return_400_when_size_exceeds_maximum() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "10")
                        .param("size", "500"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(productRepository);
    }

    @Test
    void should_return_400_when_size_is_zero() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(productRepository);
    }

    @Test
    void should_return_product_stock_summary() throws Exception {

        ProductStockSummaryView summary = productStockSummary();

        given(getProductStockLevelsUseCase.execute(any(GetProductStockLevelsQuery.class)))
                .willReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/products/{productId}/stock-levels", productId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.productName").value("Oil Filter"))
                .andExpect(jsonPath("$.globalQuantity").value(7))
                .andExpect(jsonPath("$.minimumGlobalThreshold").value(10))
                .andExpect(jsonPath("$.belowGlobalThreshold").value(Boolean.TRUE))
                .andExpect(jsonPath("$.byLocation.length()").value(2))
                .andExpect(jsonPath("$.byLocation[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.byLocation[0].productName").value("Oil Filter"))
                .andExpect(jsonPath("$.byLocation[0].locationId").value(shopFloorId.toString()))
                .andExpect(jsonPath("$.byLocation[0].locationName").value("Shop floor"))
                .andExpect(jsonPath("$.byLocation[0].locationType").value("SHOP_FLOOR"))
                .andExpect(jsonPath("$.byLocation[0].shopId").value(shopId.toString()))
                .andExpect(jsonPath("$.byLocation[1].locationId").value(backstockId.toString()))
                .andExpect(jsonPath("$.byLocation[0].quantity").value(3))
                .andExpect(jsonPath("$.byLocation[1].quantity").value(4));

        ArgumentCaptor<GetProductStockLevelsQuery> queryCaptor = ArgumentCaptor.forClass(GetProductStockLevelsQuery.class);
        verify(getProductStockLevelsUseCase).execute(queryCaptor.capture());
        GetProductStockLevelsQuery query = queryCaptor.getValue();

        assertThat(query.productId()).isEqualTo(ProductId.of(productId));
        assertThat(query.shopId()).isNull();


    }

    @Test
    void should_pass_shop_id_to_product_stock_summary_use_case() throws Exception {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID shopId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID shopFloorId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID backstockId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        ProductStockSummaryView summary = productStockSummary();

        given(getProductStockLevelsUseCase.execute(any(GetProductStockLevelsQuery.class)))
                .willReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/products/{productId}/stock-levels", productId)
                        .param("shopId", shopId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId.toString()));

        ArgumentCaptor<GetProductStockLevelsQuery> queryCaptor =
                ArgumentCaptor.forClass(GetProductStockLevelsQuery.class);

        verify(getProductStockLevelsUseCase).execute(queryCaptor.capture());

        GetProductStockLevelsQuery query = queryCaptor.getValue();

        assertThat(query.productId()).isEqualTo(ProductId.of(productId));
        assertThat(query.shopId()).isEqualTo(ShopId.of(shopId));
    }

    @Test
    void should_return_404_when_product_stock_summary_does_not_exist() throws Exception {
        given(getProductStockLevelsUseCase.execute(any(GetProductStockLevelsQuery.class)))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/{productId}/stock-levels", productId))
                .andExpect(status().isNotFound());

        ArgumentCaptor<GetProductStockLevelsQuery> queryCaptor =
                ArgumentCaptor.forClass(GetProductStockLevelsQuery.class);

        verify(getProductStockLevelsUseCase).execute(queryCaptor.capture());

        GetProductStockLevelsQuery query = queryCaptor.getValue();

        assertThat(query.productId()).isEqualTo(ProductId.of(productId));
        assertThat(query.shopId()).isNull();
    }

    @Test
    void should_return_400_when_product_stock_summary_product_id_is_not_uuid() throws Exception {
        mockMvc.perform(get("/api/v1/products/not-uuid/stock-levels"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getProductStockLevelsUseCase);
    }

    @Test
    void should_return_400_when_product_stock_summary_shop_id_is_not_uuid() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}/stock-levels", productId)
                        .param("shopId", "not-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getProductStockLevelsUseCase);
    }

    private ProductStockSummaryView productStockSummary(
    ) {
        return new ProductStockSummaryView(
                ProductId.of(productId),
                "Oil Filter",
                7,
                10,
                true,
                List.of(
                        new StockLevelView(
                                ProductId.of(productId),
                                "Oil Filter",
                                LocationId.of(shopFloorId),
                                "Shop floor",
                                LocationType.SHOP_FLOOR,
                                ShopId.of(shopId),
                                3
                        ),
                        new StockLevelView(
                                ProductId.of(productId),
                                "Oil Filter",
                                LocationId.of(backstockId),
                                "Backstock",
                                LocationType.BACKSTOCK,
                                ShopId.of(shopId),
                                4
                        )
                )
        );
    }

    private Product sampleProduct(int index) {
        UUID productId = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", index));
        UUID categoryId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        return new Product(
                ProductId.of(productId),
                "Product " + index,
                "REF-" + String.format("%03d", index),
                CategoryId.of(categoryId),
                10,
                Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"))
        );
    }
}
