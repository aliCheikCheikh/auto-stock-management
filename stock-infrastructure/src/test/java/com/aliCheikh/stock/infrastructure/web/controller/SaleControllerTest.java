package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.application.usecase.ListSalesUseCase;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SaleController.class)
@AutoConfigureMockMvc(addFilters = false)
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SellProductUseCase sellProductUseCase;

    @MockitoBean
    private SaleRepository saleRepository;

    @MockitoBean
    private ListSalesUseCase listSalesUseCase;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    private UUID saleId;
    private UUID productId;
    private UUID userId;
    private UUID shopId;
    private SaleLineDto singleLine;
    private LocalDateTime createdAt;
    private String expectedCreatedAt;
    private Money unitPrice;

    @BeforeEach
    void setUp() {
        expectedCreatedAt = "2026-05-15T10:30:00";
        saleId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        productId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        shopId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        createdAt = LocalDateTime.of(2026, 5, 15, 10, 30);
        unitPrice = Money.create(new BigDecimal("15.00"), Currency.getInstance("EUR"));
        singleLine = new SaleLineDto(
                ProductId.of(productId),
                4,
                unitPrice,
                unitPrice.multiply(4));
    }

    @Test
    void should_return_201_when_new_valid_sale_is_created() throws Exception {
        given(sellProductUseCase.sell(any(SellProductCommand.class)))
                .willReturn(new SellProductResult(
                        SaleId.of(saleId),
                        UserId.of(userId),
                        List.of(singleLine),
                        singleLine.lineTotal(),
                        createdAt
                ));

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sellerId": "%s",
                                  "shopId": "%s",
                                  "lines": [
                                    {
                                      "productId": "%s",
                                      "quantity": 4
                                    }
                                  ]
                                }
                                """.formatted(userId, shopId, productId)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.sellerId").value(userId.toString()))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.lines[0].quantity").value(4))
                .andExpect(jsonPath("$.lines[0].unitPrice.amount").value("15.00"))
                .andExpect(jsonPath("$.lines[0].unitPrice.currency").value("EUR"))
                .andExpect(jsonPath("$.lines[0].subtotal.amount").value("60.00"))
                .andExpect(jsonPath("$.lines[0].subtotal.currency").value("EUR"))
                .andExpect(jsonPath("$.totalAmount.amount").value("60.00"))
                .andExpect(jsonPath("$.totalAmount.currency").value("EUR"))
                .andExpect(jsonPath("$.createdAt").value(expectedCreatedAt));

        ArgumentCaptor<SellProductCommand> captor = ArgumentCaptor.forClass(SellProductCommand.class);
        verify(sellProductUseCase).sell(captor.capture());

        SellProductCommand command = captor.getValue();
        assertThat(command.shopId()).isEqualTo(ShopId.of(shopId));
        assertThat(command.sellerId()).isEqualTo(UserId.of(userId));
        assertThat(command.lines()).hasSize(1);
        assertThat(command.lines().get(0).productId()).isEqualTo(ProductId.of(productId));
        assertThat(command.lines().get(0).quantity()).isEqualTo(4);
    }

    @Test
    void should_return_400_when_body_is_empty() throws Exception {
        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sellProductUseCase);
    }

    @Test
    void should_return_400_when_seller_id_is_missing() throws Exception {
        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopId": "%s",
                                  "lines": [
                                    {
                                      "productId": "%s",
                                      "quantity": 4
                                    }
                                  ]
                                }
                                """.formatted(shopId, productId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sellProductUseCase);
    }

    @Test
    void should_return_400_when_sale_lines_are_empty() throws Exception {
        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sellerId": "%s",
                                  "shopId": "%s",
                                  "lines": []
                                }
                                """.formatted(userId, shopId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sellProductUseCase);
    }

    @Test
    void should_return_400_when_sale_line_quantity_is_not_positive() throws Exception {
        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sellerId": "%s",
                                  "shopId": "%s",
                                  "lines": [
                                    {
                                      "productId": "%s",
                                      "quantity": 0
                                    }
                                  ]
                                }
                                """.formatted(userId, shopId, productId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(sellProductUseCase);
    }

    @Test
    void should_return_201_when_sale_contains_multiple_lines() throws Exception {
        UUID secondProductId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        SaleLineDto firstLine = new SaleLineDto(
                ProductId.of(productId),
                4,
                Money.create(new BigDecimal("15.00"), Currency.getInstance("EUR")),
                Money.create(new BigDecimal("60.00"), Currency.getInstance("EUR"))
        );

        SaleLineDto secondLine = new SaleLineDto(
                ProductId.of(secondProductId),
                2,
                Money.create(new BigDecimal("5.00"), Currency.getInstance("EUR")),
                Money.create(new BigDecimal("10.00"), Currency.getInstance("EUR"))
        );

        given(sellProductUseCase.sell(any(SellProductCommand.class)))
                .willReturn(new SellProductResult(
                        SaleId.of(saleId),
                        UserId.of(userId),
                        List.of(firstLine, secondLine),
                        Money.create(new BigDecimal("70.00"), Currency.getInstance("EUR")),
                        createdAt
                ));

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sellerId": "%s",
                                  "shopId": "%s",
                                  "lines": [
                                    {
                                      "productId": "%s",
                                      "quantity": 4
                                    },
                                    {
                                      "productId": "%s",
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """.formatted(userId, shopId, productId, secondProductId)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.sellerId").value(userId.toString()))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.lines[0].quantity").value(4))
                .andExpect(jsonPath("$.lines[0].unitPrice.amount").value("15.00"))
                .andExpect(jsonPath("$.lines[0].subtotal.amount").value("60.00"))
                .andExpect(jsonPath("$.lines[1].productId").value(secondProductId.toString()))
                .andExpect(jsonPath("$.lines[1].quantity").value(2))
                .andExpect(jsonPath("$.lines[1].unitPrice.amount").value("5.00"))
                .andExpect(jsonPath("$.lines[1].subtotal.amount").value("10.00"))
                .andExpect(jsonPath("$.totalAmount.amount").value("70.00"));

        ArgumentCaptor<SellProductCommand> captor = ArgumentCaptor.forClass(SellProductCommand.class);
        verify(sellProductUseCase).sell(captor.capture());

        SellProductCommand command = captor.getValue();
        assertThat(command.shopId()).isEqualTo(ShopId.of(shopId));
        assertThat(command.sellerId()).isEqualTo(UserId.of(userId));
        assertThat(command.lines()).hasSize(2);
        assertThat(command.lines().get(0).productId()).isEqualTo(ProductId.of(productId));
        assertThat(command.lines().get(0).quantity()).isEqualTo(4);
        assertThat(command.lines().get(1).productId()).isEqualTo(ProductId.of(secondProductId));
        assertThat(command.lines().get(1).quantity()).isEqualTo(2);
    }

    @Test
    void should_return_200_when_sale_exists() throws Exception {
        SaleLineDto line = new SaleLineDto(ProductId.of(productId), 4, unitPrice, unitPrice.multiply(4));
        Sale sale = Sale.rehydrate(
                SaleId.of(saleId),
                UserId.of(userId),
                createdAt,
                unitPrice.multiply(4),
                List.of(line)
        );

        when(saleRepository.findById(SaleId.of(saleId))).thenReturn(Optional.of(sale));

        mockMvc.perform(get("/api/v1/sales/{saleId}", saleId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.sellerId").value(userId.toString()))
                .andExpect(jsonPath("$.lines").isArray())
                .andExpect(jsonPath("$.lines").isNotEmpty())
                .andExpect(jsonPath("$.lines[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.lines[0].quantity").value(4))
                .andExpect(jsonPath("$.lines[0].unitPrice.amount").value("15.00"))
                .andExpect(jsonPath("$.lines[0].subtotal.amount").value("60.00"))
                .andExpect(jsonPath("$.totalAmount.amount").value("60.00"))
                .andExpect(jsonPath("$.createdAt").value("2026-05-15T10:30:00"));
    }

    @Test
    void should_return_404_when_sale_does_not_exist() throws Exception {
        when(saleRepository.findById(SaleId.of(saleId))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/sales/{saleId}", saleId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Sale not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("SALE_NOT_FOUND"));
    }

    @Test
    void should_return_400_when_sale_id_is_not_uuid() throws Exception {
        mockMvc.perform(get("/api/v1/sales/not-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(saleRepository);
    }

    @Test
    void should_return_sale_page() throws Exception {
        SaleView saleView = new SaleView(SaleId.of(saleId),
                UserId.of(userId),
                List.of(singleLine),
                unitPrice.multiply(4),
                createdAt);
        given(listSalesUseCase.execute(any(ListSalesQuery.class))).willReturn(new PageResult<>(
                List.of(saleView),
                0,
                20,
                1,
                1
        ));

        mockMvc.perform(get("/api/v1/sales"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].sellerId").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].lines").isArray())
                .andExpect(jsonPath("$.content[0].lines").isNotEmpty())
                .andExpect(jsonPath("$.content[0].lines[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.content[0].lines[0].quantity").value(4))
                .andExpect(jsonPath("$.content[0].lines[0].unitPrice.amount").value("15.00"))
                .andExpect(jsonPath("$.content[0].lines[0].subtotal.amount").value("60.00"))
                .andExpect(jsonPath("$.content[0].totalAmount.amount").value("60.00"))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-05-15T10:30:00"))
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));

        ArgumentCaptor<ListSalesQuery> queryCaptor = ArgumentCaptor.forClass(ListSalesQuery.class);
        verify(listSalesUseCase).execute(queryCaptor.capture());
        ListSalesQuery query = queryCaptor.getValue();
        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.sort()).containsExactly("createdAt,desc");
        assertThat(query.sellerId()).isNull();
        assertThat(query.shopId()).isNull();
        assertThat(query.from()).isNull();
        assertThat(query.to()).isNull();

    }

    @Test
    void should_pass_query_parameters_to_sales_listing_use_case() throws Exception {

        given(listSalesUseCase.execute(any(ListSalesQuery.class)))
                .willReturn(new PageResult<>(List.of(), 1, 10, 0, 0));

        mockMvc.perform(get("/api/v1/sales")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "createdAt,asc")
                        .param("sellerId", userId.toString())
                        .param("from", "2026-05-01T00:00:00")
                        .param("to", "2026-05-20T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.page").value(1))
                .andExpect(jsonPath("$.page.size").value(10));

        ArgumentCaptor<ListSalesQuery> queryCaptor = ArgumentCaptor.forClass(ListSalesQuery.class);
        verify(listSalesUseCase).execute(queryCaptor.capture());

        ListSalesQuery query = queryCaptor.getValue();

        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(10);
        assertThat(query.sort()).containsExactly("createdAt,asc");
        assertThat(query.sellerId()).isEqualTo(UserId.of(userId));
        assertThat(query.from()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        assertThat(query.to()).isEqualTo(LocalDateTime.of(2026, 5, 20, 0, 0));

    }

    @Test
    void should_return_400_when_sales_page_is_negative() throws Exception {
        mockMvc.perform(get("/api/v1/sales")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(listSalesUseCase);
    }

    @Test
    void should_return_400_when_sales_page_size_is_too_large() throws Exception {
        mockMvc.perform(get("/api/v1/sales")
                        .param("size", "201"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(listSalesUseCase);
    }


}
