package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;
import com.aliCheikh.stock.application.port.CreditSaleDetailQueryPort;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.model.sale.SaleId;

import java.util.Objects;

/**
 * Consulter le détail d'une créance.
 *
 * <p>Une vente au comptant est traitée comme absente plutôt que refusée : du point de vue de cet
 * écran, elle n'existe pas en tant que créance, et distinguer les deux cas ne renseignerait
 * l'appelant que sur l'existence d'une vente qui ne le concerne pas.</p>
 */
public class GetCreditSaleDetailUseCase {

    private final CreditSaleDetailQueryPort creditSaleDetailQueryPort;

    public GetCreditSaleDetailUseCase(CreditSaleDetailQueryPort creditSaleDetailQueryPort) {
        this.creditSaleDetailQueryPort = Objects.requireNonNull(
                creditSaleDetailQueryPort, "creditSaleDetailQueryPort cannot be null");
    }

    public CreditSaleDetailView detailOf(SaleId saleId) {
        Objects.requireNonNull(saleId, "saleId cannot be null");

        return creditSaleDetailQueryPort.findCreditSaleDetail(saleId.getValue())
                .orElseThrow(() -> new SaleNotFoundException(saleId));
    }
}
