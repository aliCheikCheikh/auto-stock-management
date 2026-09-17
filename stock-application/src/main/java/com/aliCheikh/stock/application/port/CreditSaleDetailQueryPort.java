package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads credit sale details with product names, seller details and payments without rebuilding the
 * aggregate.
 */
public interface CreditSaleDetailQueryPort {

    /** @return the details, or empty if the sale does not exist or has no customer */
    Optional<CreditSaleDetailView> findCreditSaleDetail(UUID saleId);
}
