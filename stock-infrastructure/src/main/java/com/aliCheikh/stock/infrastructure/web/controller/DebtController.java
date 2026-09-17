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

/** Debt list and detail endpoints. */
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

    /** Filters debts by settlement status, defaulting to OUTSTANDING. */
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
     * Credit sale details remain accessible after settlement. The /debts route applies owner-only
     * access to customer information.
     */
    @GetMapping("/{saleId}")
    public CreditSaleDetailResponse getCreditSaleDetail(@PathVariable UUID saleId) {
        return CreditSaleDetailResponse.from(getCreditSaleDetailUseCase.detailOf(SaleId.of(saleId)));
    }
}
