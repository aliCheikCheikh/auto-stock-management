package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.DebtStatus;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.usecase.GetCreditSaleDetailUseCase;
import com.aliCheikh.stock.application.usecase.ListDebtsUseCase;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.infrastructure.web.dto.CreditSaleDetailResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfDebtResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.DebtWebMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/** Consultation des créances : « qui me doit de l'argent », puis « pourquoi », puis « depuis quand ». */
@RestController
@RequestMapping("/api/v1/debts")
@Validated
public class DebtController {

    private final ListDebtsUseCase listDebtsUseCase;
    private final GetCreditSaleDetailUseCase getCreditSaleDetailUseCase;

    public DebtController(ListDebtsUseCase listDebtsUseCase,
                          GetCreditSaleDetailUseCase getCreditSaleDetailUseCase) {
        this.listDebtsUseCase = Objects.requireNonNull(listDebtsUseCase);
        this.getCreditSaleDetailUseCase = Objects.requireNonNull(getCreditSaleDetailUseCase);
    }

    /**
     * Les créances, filtrées par statut de règlement.
     *
     * <p>Le statut par défaut reste {@code OUTSTANDING} : sans paramètre, l'écran répond à la
     * question qu'on lui posait déjà, « qui me doit de l'argent ». L'historique est une demande
     * explicite, pas un effet de bord d'une requête sans filtre.</p>
     */
    @GetMapping
    public PageOfDebtResponse listDebts(
            @RequestParam(defaultValue = "OUTSTANDING") DebtStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        return DebtWebMapper.toPageResponse(
                listDebtsUseCase.execute(new ListDebtsQuery(page, size, status, customerId)));
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
