package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ReceiveStockCommand;
import com.aliCheikh.stock.application.dto.ReceiveStockResult;
import com.aliCheikh.stock.application.usecase.ReceiveStockUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.ReceiveStockRequest;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptAcknowledgementResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.ReceiveStockWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-receipts")
public class StockReceiptController {

    private final ReceiveStockUseCase receiveStockUseCase;

    public StockReceiptController(ReceiveStockUseCase receiveStockUseCase) {
        this.receiveStockUseCase = receiveStockUseCase;
    }

    @PostMapping
    public ResponseEntity<StockReceiptAcknowledgementResponse> receiveStock(@Valid @RequestBody ReceiveStockRequest request,
                                                                            Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        ReceiveStockCommand command = ReceiveStockWebMapper.toCommand(request, userId);
        ReceiveStockResult result = receiveStockUseCase.execute(command);
        StockReceiptAcknowledgementResponse response = ReceiveStockWebMapper.toResponse(result);
        return ResponseEntity.accepted().body(response);
    }
}
