package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;

public interface ListSalesQueryPort {
    PageResult<SaleView> findByQuery(ListSalesQuery query);
}
