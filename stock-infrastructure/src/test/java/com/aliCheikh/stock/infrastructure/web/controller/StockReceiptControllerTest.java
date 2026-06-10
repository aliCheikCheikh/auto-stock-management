package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.dto.ReceiveStockResult;
import com.aliCheikh.stock.application.usecase.ReceiveStockUseCase;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
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
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockReceiptController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @MockitoBean
    private ReceiveStockUseCase receiveStockUseCase;
    private UUID shopId;
    private UUID userId;
    private UUID productId;
    private UUID locationId;
    private UUID movementId;
    private UUID categoryId;
    private Instant acceptedAt;
    private String productReference;

    @BeforeEach
    void setUp() {
        productReference = "REF-001";
        shopId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        locationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        productId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        movementId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        categoryId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        acceptedAt = Instant.parse("2026-05-14T12:00:00Z");
    }

    @Test
    void should_accept_stock_receipt_for_existing_product() throws Exception {

        given(receiveStockUseCase.execute(any(ReceiveStockCommand.class)))
                .willReturn(new ReceiveStockResult(
                        ProductId.of(productId),
                        50,
                        List.of(MovementId.of(movementId)),
                        acceptedAt
                ));

        mockMvc.perform(post("/api/v1/stock-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productReference": "REF-001",
                                  "shopId": "%s",
                                  "userId": "%s",
                                  "distributions": [
                                    {
                                      "locationId": "%s",
                                      "quantity": 50
                                    }
                                  ]
                                }
                                """.formatted(shopId, userId, locationId)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.totalReceived").value(50))
                .andExpect(jsonPath("$.acceptedAt").value(acceptedAt.toString()))
                .andExpect(jsonPath("$.movementIds[0]").value(movementId.toString()));

        ArgumentCaptor<ReceiveStockCommand> commandCaptor = ArgumentCaptor.forClass(ReceiveStockCommand.class);
        verify(receiveStockUseCase).execute(commandCaptor.capture());

        ReceiveStockCommand command = commandCaptor.getValue();
        assertThat(command.productReference()).isEqualTo(productReference);
        assertThat(command.newProductInfo()).isNull();
        assertThat(command.shopId()).isEqualTo(ShopId.of(shopId));
        assertThat(command.userId()).isEqualTo(UserId.of(userId));
        assertThat(command.distributions()).hasSize(1);
        assertThat(command.distributions().get(0).locationId()).isEqualTo(LocationId.of(locationId));
        assertThat(command.distributions().get(0).quantity()).isEqualTo(50);
    }

    @Test
    void should_return_400_when_body_is_empty() throws Exception {
        mockMvc.perform(post("/api/v1/stock-receipts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(receiveStockUseCase);
    }

    @Test
    void should_return_400_when_product_reference_is_blank() throws Exception {
        mockMvc.perform(post("/api/v1/stock-receipts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                         "productReference": "",
                         "shopId": "%s",
                         "userId": "%s",
                         "distributions": [
                                    {
                                      "locationId": "%s",
                                      "quantity": 50
                                    }
                                  ]
                                }
                        """.formatted(shopId, userId, locationId)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(receiveStockUseCase);
    }

    @Test
    void should_return_400_when_distributions_is_empty() throws Exception {
        mockMvc.perform(post("/api/v1/stock-receipts").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                         "productReference": "REF-001",
                         "shopId": "%s",
                         "userId": "%s",
                         "distributions": [
                                ]
                                }
                        """.formatted(shopId, userId))).andExpect(status().isBadRequest());
        verifyNoInteractions(receiveStockUseCase);
    }

    @Test
    void should_return_400_when_distribution_quantity_is_not_positive() throws Exception {
        mockMvc.perform(post("/api/v1/stock-receipts").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                         "productReference": "REF-001",
                         "shopId": "%s",
                         "userId": "%s",
                         "distributions": [
                                   {
                                      "locationId": "%s",
                                      "quantity": 0
                                    }
                                ]
                                }
                        """.formatted(shopId, userId, locationId))).andExpect(status().isBadRequest());
        verifyNoInteractions(receiveStockUseCase);
    }

    @Test
    void should_accept_stock_receipt_with_new_product_info() throws Exception {
        given(receiveStockUseCase.execute(any(ReceiveStockCommand.class)))
                .willReturn(new ReceiveStockResult(
                        ProductId.of(productId),
                        50,
                        List.of(MovementId.of(movementId)),
                        acceptedAt
                ));

        mockMvc.perform(post("/api/v1/stock-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productReference": "REF-001",
                                  "newProductInfo": {
                                    "name": "Oil Filter",
                                    "reference": "REF-001",
                                    "categoryId": "%s",
                                    "unitPrice": {
                                      "amount": "10.00",
                                      "currency": "EUR"
                                    },
                                    "minimumGlobalThreshold": 20
                                  },
                                  "shopId": "%s",
                                  "userId": "%s",
                                  "distributions": [
                                    {
                                      "locationId": "%s",
                                      "quantity": 50
                                    }
                                  ]
                                }
                                """.formatted(categoryId, shopId, userId, locationId)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.totalReceived").value(50))
                .andExpect(jsonPath("$.acceptedAt").value(acceptedAt.toString()))
                .andExpect(jsonPath("$.movementIds[0]").value(movementId.toString()));

        ArgumentCaptor<ReceiveStockCommand> commandCaptor = ArgumentCaptor.forClass(ReceiveStockCommand.class);
        verify(receiveStockUseCase).execute(commandCaptor.capture());

        ReceiveStockCommand command = commandCaptor.getValue();
        assertThat(command.newProductInfo()).isNotNull();
        assertThat(command.newProductInfo().name()).isEqualTo("Oil Filter");
        assertThat(command.newProductInfo().reference()).isEqualTo("REF-001");
        assertThat(command.newProductInfo().categoryId()).isEqualTo(CategoryId.of(categoryId));
        assertThat(command.newProductInfo().minimumGlobalThreshold()).isEqualTo(20);
        assertThat(command.newProductInfo().unitPrice().getAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(command.newProductInfo().unitPrice().getCurrency()).isEqualTo(Currency.getInstance("EUR"));
    }


}
