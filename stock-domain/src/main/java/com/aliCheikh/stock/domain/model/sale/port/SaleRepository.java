package com.aliCheikh.stock.domain.model.sale.port;

import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleId;

import java.util.Optional;

public interface SaleRepository {

    void save(Sale sale);

    Optional<Sale> findById(SaleId saleId);

    /**
     * Loads a sale with exclusive access until the current transaction ends. Payment recording
     * requires this contract to prevent concurrent payments from exceeding the outstanding
     * balance.
     */
    Optional<Sale> findByIdForUpdate(SaleId saleId);
}
