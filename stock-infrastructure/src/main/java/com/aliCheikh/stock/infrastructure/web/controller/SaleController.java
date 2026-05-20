package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.application.usecase.ListSalesUseCase;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleRequest;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfSaleResponse;
import com.aliCheikh.stock.infrastructure.web.dto.SaleResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.SaleWebMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sales")
@Validated
public class SaleController {

    private final SellProductUseCase sellProductUseCase;
    private final SaleRepository saleRepository;
    private final ListSalesUseCase listSalesUseCase;

    public SaleController(
            SellProductUseCase sellProductUseCase,
            SaleRepository saleRepository,
            ListSalesUseCase listSalesUseCase
    ) {
        this.sellProductUseCase = sellProductUseCase;
        this.saleRepository = saleRepository;
        this.listSalesUseCase = listSalesUseCase;
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
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<PageOfSaleResponse> getSales(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        ListSalesQuery query = SaleWebMapper.toQuery(
                page,
                size,
                queryParams.get("sort"),
                sellerId,
                shopId,
                from,
                to);
        PageResult<SaleView> view = listSalesUseCase.execute(query);
        PageOfSaleResponse response = SaleWebMapper.toPageResponse(view);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Void> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.badRequest().build();
    }
}
