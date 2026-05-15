package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

@WebMvcTest(SaleController.class)
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SellProductUseCase sellProductUseCase;

    private UUID saleId;
    private UUID productId;
    private UUID userId;
    private UUID shopId;
    private SaleLineDto singleLine;
    private LocalDateTime createdAt;
    private String expectedCreatedAt;

    @BeforeEach
    void setUp() {
        expectedCreatedAt = "2026-05-15T10:30:00";
        saleId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        productId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        shopId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        createdAt = LocalDateTime.of(2026, 5, 15, 10, 30);

        singleLine = new SaleLineDto(
                ProductId.of(productId),
                4,
                Money.create(new BigDecimal("15.00"), Currency.getInstance("EUR")),
                Money.create(new BigDecimal("60.00"), Currency.getInstance("EUR"))
        );
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


}