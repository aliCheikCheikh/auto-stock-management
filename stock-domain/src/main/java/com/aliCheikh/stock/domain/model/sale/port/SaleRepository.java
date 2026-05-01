package com.aliCheikh.stock.domain.model.sale.port;

import com.aliCheikh.stock.domain.model.sale.Sale;

public interface SaleRepository {
    void save(Sale sale);
}
