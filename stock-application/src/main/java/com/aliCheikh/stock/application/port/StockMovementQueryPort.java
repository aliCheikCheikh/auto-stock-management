package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.ListStockMovementsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockMovementView;

public interface StockMovementQueryPort {
    PageResult<StockMovementView> findByQuery(ListStockMovementsQuery query);
}
