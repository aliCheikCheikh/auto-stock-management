package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;

public interface StockLevelQueryPort {
    PageResult<StockLevelView> findByQuery(ListStockLevelsQuery query);
}
