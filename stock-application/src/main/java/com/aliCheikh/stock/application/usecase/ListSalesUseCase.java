package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.application.port.ListSalesQueryPort;

import java.util.Objects;

public class ListSalesUseCase {
    private final ListSalesQueryPort listSalesQueryPort;

    public ListSalesUseCase(ListSalesQueryPort listSalesQueryPort) {
        this.listSalesQueryPort = Objects.requireNonNull(listSalesQueryPort, "listSalesQueryPort cannot be null");
    }

    public PageResult<SaleView> execute(ListSalesQuery query) {
        Objects.requireNonNull(query, "query cannot be null");
        return listSalesQueryPort.findByQuery(query);
    }
}
