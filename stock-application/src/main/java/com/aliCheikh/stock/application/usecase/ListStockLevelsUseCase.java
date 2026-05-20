package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.port.StockLevelQueryPort;

import java.util.Objects;

public class ListStockLevelsUseCase {
    private final StockLevelQueryPort stockLevelQueryPort;

    public ListStockLevelsUseCase(StockLevelQueryPort stockLevelQueryPort) {
        this.stockLevelQueryPort = Objects.requireNonNull(stockLevelQueryPort, "stockLevelQueryPort cannot be null");
    }

    public PageResult<StockLevelView> execute(ListStockLevelsQuery query) {
        return stockLevelQueryPort.findByQuery(query);
    }
}
