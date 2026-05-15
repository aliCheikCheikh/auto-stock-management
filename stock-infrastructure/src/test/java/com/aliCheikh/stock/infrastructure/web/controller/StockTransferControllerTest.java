package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.TransferStockCommand;
import com.aliCheikh.stock.application.dto.TransferStockResult;
import com.aliCheikh.stock.application.usecase.TransferStockUseCase;
import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockTransferController.class)
class StockTransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferStockUseCase transferStockUseCase;

    private UUID productId;
    private UUID sourceLocationId;
    private UUID destinationLocationId;
    private UUID userId;
    private UUID movementId;
    private Instant acceptedAt;

    @BeforeEach
    void setUp() {
        productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        sourceLocationId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        destinationLocationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        movementId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        acceptedAt = Instant.parse("2026-05-15T12:00:00Z");
    }

    @Test
    void should_accept_stock_transfer() throws Exception {
        given(transferStockUseCase.execute(any(TransferStockCommand.class)))
                .willReturn(new TransferStockResult(
                        MovementId.of(movementId),
                        acceptedAt
                ));

        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sourceLocationId": "%s",
                                  "destinationLocationId": "%s",
                                  "quantity": 5,
                                  "userId": "%s"
                                }
                                """.formatted(productId, sourceLocationId, destinationLocationId, userId)))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.movementId").value(movementId.toString()))
                .andExpect(jsonPath("$.acceptedAt").value(acceptedAt.toString()));

        ArgumentCaptor<TransferStockCommand> commandCaptor = ArgumentCaptor.forClass(TransferStockCommand.class);
        verify(transferStockUseCase).execute(commandCaptor.capture());

        TransferStockCommand command = commandCaptor.getValue();
        assertThat(command.productId()).isEqualTo(ProductId.of(productId));
        assertThat(command.sourceLocationId()).isEqualTo(LocationId.of(sourceLocationId));
        assertThat(command.destinationLocationId()).isEqualTo(LocationId.of(destinationLocationId));
        assertThat(command.quantity()).isEqualTo(5);
        assertThat(command.userId()).isEqualTo(UserId.of(userId));
    }

    @Test
    void should_return_400_when_body_is_empty() throws Exception {
        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferStockUseCase);
    }

    @Test
    void should_return_400_when_product_id_is_missing() throws Exception {
        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceLocationId": "%s",
                                  "destinationLocationId": "%s",
                                  "quantity": 5,
                                  "userId": "%s"
                                }
                                """.formatted(sourceLocationId, destinationLocationId, userId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferStockUseCase);
    }

    @Test
    void should_return_400_when_source_location_id_is_missing() throws Exception {
        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "destinationLocationId": "%s",
                                  "quantity": 5,
                                  "userId": "%s"
                                }
                                """.formatted(productId, destinationLocationId, userId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferStockUseCase);
    }

    @Test
    void should_return_400_when_destination_location_id_is_missing() throws Exception {
        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sourceLocationId": "%s",
                                  "quantity": 5,
                                  "userId": "%s"
                                }
                                """.formatted(productId, sourceLocationId, userId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferStockUseCase);
    }

    @Test
    void should_return_400_when_quantity_is_not_positive() throws Exception {
        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sourceLocationId": "%s",
                                  "destinationLocationId": "%s",
                                  "quantity": 0,
                                  "userId": "%s"
                                }
                                """.formatted(productId, sourceLocationId, destinationLocationId, userId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferStockUseCase);
    }

    @Test
    void should_return_400_when_user_id_is_missing() throws Exception {
        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sourceLocationId": "%s",
                                  "destinationLocationId": "%s",
                                  "quantity": 5
                                }
                                """.formatted(productId, sourceLocationId, destinationLocationId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transferStockUseCase);
    }
}
