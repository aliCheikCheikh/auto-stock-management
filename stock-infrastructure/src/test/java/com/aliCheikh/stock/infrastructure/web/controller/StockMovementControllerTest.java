package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ListStockMovementsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockMovementView;
import com.aliCheikh.stock.application.usecase.ListStockMovementsUseCase;
import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.OperationId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.shared.Money;
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
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockMovementController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockMovementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListStockMovementsUseCase listStockMovementsUseCase;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    private UUID movementId;
    private UUID productId;
    private UUID sourceLocationId;
    private UUID destinationLocationId;
    private UUID userId;
    private LocalDateTime executedAt;

    @BeforeEach
    void setUp() {
        movementId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        productId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        sourceLocationId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        destinationLocationId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        executedAt = LocalDateTime.of(2026, 5, 15, 12, 0);
    }

    @Test
    void should_return_stock_movements_page() throws Exception {
        StockMovementView movement = new StockMovementView(
                MovementId.of(movementId),
                ProductId.of(productId),
                LocationId.of(sourceLocationId),
                LocationId.of(destinationLocationId),
                MovementType.TRANSFER,
                5,
                UserId.of(userId),
                "Ahmat",
                executedAt,
                null,
                OperationId.generate(),
                null);

        given(listStockMovementsUseCase.execute(any(ListStockMovementsQuery.class)))
                .willReturn(new PageResult<>(
                        List.of(movement),
                        0,
                        20,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/v1/stock-movements"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].movementId").value(movementId.toString()))
                .andExpect(jsonPath("$.content[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.content[0].locationId").value(sourceLocationId.toString()))
                .andExpect(jsonPath("$.content[0].destinationLocationId").value(destinationLocationId.toString()))
                .andExpect(jsonPath("$.content[0].type").value("TRANSFER"))
                // L'utilisateur attend un nom, pas un identifiant : « c'est Ahmat qui a fait ça ».
                .andExpect(jsonPath("$.content[0].executedByName").value("Ahmat"))
                .andExpect(jsonPath("$.content[0].operationId").isNotEmpty())
                .andExpect(jsonPath("$.content[0].quantity").value(5))
                .andExpect(jsonPath("$.content[0].executedBy").value(userId.toString()))
                .andExpect(jsonPath("$.content[0].executedAt").value("2026-05-15T12:00:00"))
                .andExpect(jsonPath("$.content[0].saleId").doesNotExist())
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));

        ArgumentCaptor<ListStockMovementsQuery> queryCaptor = ArgumentCaptor.forClass(ListStockMovementsQuery.class);
        verify(listStockMovementsUseCase).execute(queryCaptor.capture());

        ListStockMovementsQuery query = queryCaptor.getValue();
        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.sort()).containsExactly("executedAt,desc");
        assertThat(query.productId()).isNull();
        assertThat(query.locationId()).isNull();
        assertThat(query.type()).isNull();
        assertThat(query.from()).isNull();
        assertThat(query.to()).isNull();
    }

    /**
     * « Cette vente, elle a été payée ou pas ? » — l'historique doit répondre sans quitter l'écran.
     * Le solde restant porte l'information : zéro signifie réglée, une valeur positive signifie à
     * crédit. Un booléen supplémentaire n'apporterait rien et pourrait le contredire.
     */
    @Test
    void should_expose_the_remaining_balance_of_the_sale_behind_a_movement() throws Exception {
        UUID saleId = UUID.randomUUID();

        given(listStockMovementsUseCase.execute(any(ListStockMovementsQuery.class)))
                .willReturn(new PageResult<>(List.of(saleMovement(saleId, xaf("30000"))), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/stock-movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleId").value(saleId.toString()))
                .andExpect(jsonPath("$.content[0].saleAmountDue.amount").value("30000"))
                .andExpect(jsonPath("$.content[0].saleAmountDue.currency").value("XAF"));
    }

    @Test
    void should_expose_a_zero_balance_for_a_sale_paid_in_full() throws Exception {
        given(listStockMovementsUseCase.execute(any(ListStockMovementsQuery.class)))
                .willReturn(new PageResult<>(
                        List.of(saleMovement(UUID.randomUUID(), xaf("0"))), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/stock-movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleAmountDue.amount").value("0"));
    }

    /** Une réception ou un transfert ne naît d'aucune vente : aucun solde à annoncer. */
    @Test
    void should_omit_the_balance_when_the_movement_is_not_a_sale() throws Exception {
        given(listStockMovementsUseCase.execute(any(ListStockMovementsQuery.class)))
                .willReturn(new PageResult<>(
                        List.of(saleMovement(null, null)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/stock-movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].saleAmountDue").doesNotExist());
    }

    private StockMovementView saleMovement(UUID saleId, Money amountDue) {
        return new StockMovementView(
                MovementId.of(movementId),
                ProductId.of(productId),
                LocationId.of(sourceLocationId),
                null,
                MovementType.EXIT,
                2,
                UserId.of(userId),
                "Ahmat",
                executedAt,
                saleId == null ? null : SaleId.of(saleId),
                OperationId.generate(),
                amountDue);
    }

    private static Money xaf(String amount) {
        return Money.create(new BigDecimal(amount), Currency.getInstance("XAF"));
    }

    @Test
    void should_apply_filters_and_pagination() throws Exception {
        given(listStockMovementsUseCase.execute(any(ListStockMovementsQuery.class)))
                .willReturn(new PageResult<>(List.of(), 1, 10, 0, 0));

        mockMvc.perform(get("/api/v1/stock-movements")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "quantity,asc")
                        .param("productId", productId.toString())
                        .param("locationId", sourceLocationId.toString())
                        .param("type", "EXIT")
                        .param("from", "2026-05-01T00:00:00")
                        .param("to", "2026-05-15T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.page").value(1))
                .andExpect(jsonPath("$.page.size").value(10));

        ArgumentCaptor<ListStockMovementsQuery> queryCaptor = ArgumentCaptor.forClass(ListStockMovementsQuery.class);
        verify(listStockMovementsUseCase).execute(queryCaptor.capture());

        ListStockMovementsQuery query = queryCaptor.getValue();
        assertThat(query.page()).isEqualTo(1);
        assertThat(query.size()).isEqualTo(10);
        assertThat(query.sort()).containsExactly("quantity,asc");
        assertThat(query.productId()).isEqualTo(ProductId.of(productId));
        assertThat(query.locationId()).isEqualTo(LocationId.of(sourceLocationId));
        assertThat(query.type()).isEqualTo(MovementType.EXIT);
        assertThat(query.from()).isEqualTo(LocalDateTime.of(2026, 5, 1, 0, 0));
        assertThat(query.to()).isEqualTo(LocalDateTime.of(2026, 5, 15, 23, 59));
    }

    @Test
    void should_return_400_when_page_is_negative() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listStockMovementsUseCase);
    }

    @Test
    void should_return_400_when_size_is_zero() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements")
                        .param("page", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listStockMovementsUseCase);
    }

    @Test
    void should_return_400_when_size_exceeds_maximum() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements")
                        .param("page", "0")
                        .param("size", "500"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listStockMovementsUseCase);
    }

    @Test
    void should_return_400_when_type_is_unknown() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements")
                        .param("type", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listStockMovementsUseCase);
    }
}
