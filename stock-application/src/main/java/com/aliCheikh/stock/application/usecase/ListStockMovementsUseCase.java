package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ListStockMovementsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockMovementView;
import com.aliCheikh.stock.application.port.StockMovementQueryPort;

import java.util.Objects;

public class ListStockMovementsUseCase {

    private final StockMovementQueryPort stockMovementQueryPort;

    public ListStockMovementsUseCase(StockMovementQueryPort stockMovementQueryPort) {
        this.stockMovementQueryPort = Objects.requireNonNull(
                stockMovementQueryPort,
                "stockMovementQueryPort cannot be null"
        );
    }

    public PageResult<StockMovementView> execute(ListStockMovementsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        return stockMovementQueryPort.findByQuery(query);
    }
}
