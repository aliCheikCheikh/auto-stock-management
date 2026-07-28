package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.usecase.ListOutstandingDebtsUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.OutstandingDebtResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** Consultation globale des créances : « qui me doit de l'argent ». */
@RestController
@RequestMapping("/api/v1/debts")
public class OutstandingDebtController {

    private final ListOutstandingDebtsUseCase listOutstandingDebtsUseCase;

    public OutstandingDebtController(ListOutstandingDebtsUseCase listOutstandingDebtsUseCase) {
        this.listOutstandingDebtsUseCase = Objects.requireNonNull(listOutstandingDebtsUseCase);
    }

    @GetMapping
    public List<OutstandingDebtResponse> getOutstandingDebts() {
        return listOutstandingDebtsUseCase.listAll().stream()
                .map(OutstandingDebtResponse::from)
                .toList();
    }
}
