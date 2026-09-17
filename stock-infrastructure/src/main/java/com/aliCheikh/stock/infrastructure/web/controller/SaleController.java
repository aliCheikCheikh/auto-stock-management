package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.RecordPaymentResult;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.application.usecase.ListSalesUseCase;
import com.aliCheikh.stock.application.usecase.RecordPaymentUseCase;
import com.aliCheikh.stock.application.usecase.SellProductUseCase;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleRequest;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfSaleResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PaymentResponse;
import com.aliCheikh.stock.infrastructure.web.dto.RecordPaymentRequest;
import com.aliCheikh.stock.infrastructure.web.dto.SaleResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.SaleWebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final RecordPaymentUseCase recordPaymentUseCase;

    public SaleController(
            SellProductUseCase sellProductUseCase,
            SaleRepository saleRepository,
            ListSalesUseCase listSalesUseCase,
            RecordPaymentUseCase recordPaymentUseCase
    ) {
        this.sellProductUseCase = sellProductUseCase;
        this.saleRepository = saleRepository;
        this.listSalesUseCase = listSalesUseCase;
        this.recordPaymentUseCase = recordPaymentUseCase;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> createSale(@Valid @RequestBody CreateSaleRequest request,
                                                   Authentication authentication) {
        UUID sellerId = (UUID) authentication.getPrincipal();
        SellProductCommand command = SaleWebMapper.toCommand(request, sellerId);
        SellProductResult result = sellProductUseCase.sell(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SaleWebMapper.toResponse(result));
    }

    /**
     * Records a payment using the authenticated user as its receiver, never a client-supplied
     * identity.
     */
    @PostMapping("/{saleId}/payments")
    public ResponseEntity<PaymentResponse> recordPayment(@PathVariable UUID saleId,
                                                         @Valid @RequestBody RecordPaymentRequest request,
                                                         Authentication authentication) {
        UUID receivedBy = (UUID) authentication.getPrincipal();
        RecordPaymentResult result = recordPaymentUseCase.record(
                SaleWebMapper.toCommand(saleId, request, receivedBy));

        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(result));
    }

    @GetMapping("/{saleId}")
    public ResponseEntity<SaleResponse> getSale(@PathVariable UUID saleId) {
        SaleId id = SaleId.of(saleId);

        return saleRepository.findById(id)
                .map(SaleWebMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new SaleNotFoundException(id));
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

}
