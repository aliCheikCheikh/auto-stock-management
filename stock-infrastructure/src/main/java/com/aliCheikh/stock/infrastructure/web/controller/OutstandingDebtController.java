package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.usecase.GetCreditSaleDetailUseCase;
import com.aliCheikh.stock.application.usecase.ListOutstandingDebtsUseCase;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.infrastructure.web.dto.CreditSaleDetailResponse;
import com.aliCheikh.stock.infrastructure.web.dto.OutstandingDebtResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Consultation des créances : « qui me doit de l'argent », puis « pourquoi ». */
@RestController
@RequestMapping("/api/v1/debts")
public class OutstandingDebtController {

    private final ListOutstandingDebtsUseCase listOutstandingDebtsUseCase;
    private final GetCreditSaleDetailUseCase getCreditSaleDetailUseCase;

    public OutstandingDebtController(ListOutstandingDebtsUseCase listOutstandingDebtsUseCase,
                                     GetCreditSaleDetailUseCase getCreditSaleDetailUseCase) {
        this.listOutstandingDebtsUseCase = Objects.requireNonNull(listOutstandingDebtsUseCase);
        this.getCreditSaleDetailUseCase = Objects.requireNonNull(getCreditSaleDetailUseCase);
    }

    @GetMapping
    public List<OutstandingDebtResponse> getOutstandingDebts() {
        return listOutstandingDebtsUseCase.listAll().stream()
                .map(OutstandingDebtResponse::from)
                .toList();
    }

    /**
     * Le détail reste accessible après règlement complet : le patron doit pouvoir justifier une
     * créance soldée autant qu'une créance en cours. Filtrer sur le solde restant reviendrait à
     * effacer la preuve au moment précis où elle devient utile.
     *
     * <p>La route vit sous {@code /debts} — et non sous {@code /sales} — parce qu'elle expose le
     * nom et le téléphone du client : la règle de sécurité qui réserve les créances au patron
     * s'applique alors sans qu'il faille y penser.</p>
     */
    @GetMapping("/{saleId}")
    public CreditSaleDetailResponse getCreditSaleDetail(@PathVariable UUID saleId) {
        return CreditSaleDetailResponse.from(getCreditSaleDetailUseCase.detailOf(SaleId.of(saleId)));
    }
}
