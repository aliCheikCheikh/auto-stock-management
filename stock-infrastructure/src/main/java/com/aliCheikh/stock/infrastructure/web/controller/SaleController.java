package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.infrastructure.persistence.mapper.SaleJpaMapper;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleRequest;
import com.aliCheikh.stock.infrastructure.web.dto.SaleResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.SaleWebMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {


    private final SellProductUseCase sellProductUseCase;
    private final SaleRepository saleRepository;

    public SaleController(SellProductUseCase sellProductUseCase, SaleRepository saleRepository) {
        this.sellProductUseCase = sellProductUseCase;
        this.saleRepository = saleRepository;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody CreateSaleRequest request) {
        SellProductCommand command = SaleWebMapper.toCommand(request);
        SellProductResult result = sellProductUseCase.sell(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SaleWebMapper.toResponse(result));
    }

    @GetMapping("/{saleId}")
    public ResponseEntity<SaleResponse> getSale(@PathVariable UUID saleId) {
        return saleRepository.findById(SaleId.of(saleId))
                .map(SaleWebMapper::toResponse)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

}
