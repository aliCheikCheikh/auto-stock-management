package com.aliCheikh.stock.infrastructure.web.filter;

import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IdempotencyFilterIntegrationTest {

    private static final String SALES_ENDPOINT = "/api/v1/sales";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SELLER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SHOP_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PRODUCT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID SALE_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("auto_stock_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdempotencyRecordJpaRepository idempotencyRepository;

    @MockitoBean
    private SellProductUseCase sellProductUseCase;

    @BeforeEach
    void setUp() {
        given(sellProductUseCase.sell(any())).willReturn(stubbedSaleResult());
        idempotencyRepository.deleteAll();
    }

    @Test
    void should_execute_once_and_store_single_record_when_key_is_replayed() throws Exception {
        String saleRequest = saleRequest(4);

        performSaleWithKey(saleRequest).andExpect(status().isCreated());
        performSaleWithKey(saleRequest).andExpect(status().isCreated());

        verify(sellProductUseCase, times(1)).sell(any());
        assertThat(idempotencyRepository.count()).isEqualTo(1);
    }

    @Test
    void should_reject_with_422_when_key_is_reused_with_different_body() throws Exception {
        performSaleWithKey(saleRequest(4)).andExpect(status().isCreated());

        performSaleWithKey(saleRequest(99))
                .andExpect(status().isUnprocessableEntity());

        verify(sellProductUseCase, times(1)).sell(any());
    }

    @Test
    void should_not_store_record_when_no_idempotency_key_is_provided() throws Exception {
        mockMvc.perform(post(SALES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saleRequest(4)))
                .andExpect(status().isCreated());

        assertThat(idempotencyRepository.count()).isZero();
    }

    private ResultActions performSaleWithKey(String body) throws Exception {
        return mockMvc.perform(post(SALES_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header(IDEMPOTENCY_KEY_HEADER, IDEMPOTENCY_KEY.toString())
                .content(body));
    }

    private String saleRequest(int quantity) {
        return """
                {"sellerId":"%s","shopId":"%s","lines":[{"productId":"%s","quantity":%d}]}
                """.formatted(SELLER_ID, SHOP_ID, PRODUCT_ID, quantity);
    }

    private SellProductResult stubbedSaleResult() {
        Money unitPrice = Money.create(new BigDecimal("10.00"), Currency.getInstance("EUR"));
        Money subtotal = Money.create(new BigDecimal("40.00"), Currency.getInstance("EUR"));

        return new SellProductResult(
                SaleId.of(SALE_ID),
                UserId.of(SELLER_ID),
                List.of(new SaleLineDto(ProductId.of(PRODUCT_ID), 4, unitPrice, subtotal)),
                subtotal,
                LocalDateTime.of(2026, 6, 2, 19, 55));
    }
}
