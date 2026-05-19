package com.aliCheikh.stock.domain.model.sale.port;

import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleId;

import java.util.Optional;

public interface SaleRepository {
    void save(Sale sale);
    Optional<Sale> findById(SaleId saleId);
}
