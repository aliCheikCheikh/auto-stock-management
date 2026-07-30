package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ListStockMovementsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockMovementView;
import com.aliCheikh.stock.application.port.StockMovementQueryPort;
import com.aliCheikh.stock.domain.model.movement.MovementId;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.domain.model.movement.OperationId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListStockMovementsUseCaseTest {

    private StockMovementQueryPort stockMovementQueryPort;
    private ListStockMovementsUseCase useCase;

    @BeforeEach
    void setUp() {
        stockMovementQueryPort = mock(StockMovementQueryPort.class);
        useCase = new ListStockMovementsUseCase(stockMovementQueryPort);
    }

    @Test
    void should_list_stock_movements_matching_query() {
        ListStockMovementsQuery query = new ListStockMovementsQuery(
                0,
                20,
                List.of("executedAt,desc"),
                ProductId.generate(),
                LocationId.generate(),
                MovementType.TRANSFER,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 15, 23, 59)
        );

        StockMovementView movement = new StockMovementView(
                MovementId.generate(),
                ProductId.generate(),
                LocationId.generate(),
                LocationId.generate(),
                MovementType.TRANSFER,
                5,
                UserId.generate(),
                "Ahmat",
                LocalDateTime.of(2026, 5, 15, 12, 0),
                null,
                OperationId.generate(),
                // Un transfert ne naît d'aucune vente : aucun solde à annoncer.
                null);
        PageResult<StockMovementView> expectedPage = new PageResult<>(
                List.of(movement),
                0,
                20,
                1,
                1
        );

        when(stockMovementQueryPort.findByQuery(query)).thenReturn(expectedPage);

        PageResult<StockMovementView> result = useCase.execute(query);

        assertThat(result).isEqualTo(expectedPage);
        verify(stockMovementQueryPort).findByQuery(query);
    }
}
