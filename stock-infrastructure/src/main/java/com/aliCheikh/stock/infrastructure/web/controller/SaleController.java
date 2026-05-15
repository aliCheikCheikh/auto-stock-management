package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleRequest;
import com.aliCheikh.stock.infrastructure.web.dto.SaleResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.SaleWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {


    private final SellProductUseCase sellProductUseCase;

    public SaleController(SellProductUseCase sellProductUseCase) {
        this.sellProductUseCase = sellProductUseCase;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody CreateSaleRequest request) {
        SellProductCommand command = SaleWebMapper.toCommand(request);
        SellProductResult result = sellProductUseCase.sell(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SaleWebMapper.toResponse(result));
    }

}
