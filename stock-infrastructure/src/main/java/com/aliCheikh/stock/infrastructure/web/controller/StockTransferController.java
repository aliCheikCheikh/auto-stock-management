package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.TransferStockCommand;
import com.aliCheikh.stock.application.dto.TransferStockResult;
import com.aliCheikh.stock.application.usecase.TransferStockUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.StockTransferAcknowledgementResponse;
import com.aliCheikh.stock.infrastructure.web.dto.TransferStockRequest;
import com.aliCheikh.stock.infrastructure.web.mapper.TransferStockWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock-transfers")
public class StockTransferController {

    private final TransferStockUseCase transferStockUseCase;

    public StockTransferController(TransferStockUseCase transferStockUseCase) {
        this.transferStockUseCase = transferStockUseCase;
    }

    @PostMapping
    public ResponseEntity<StockTransferAcknowledgementResponse> transferStock(
            @Valid @RequestBody TransferStockRequest request
    ) {
        TransferStockCommand command = TransferStockWebMapper.toCommand(request);
        TransferStockResult result = transferStockUseCase.execute(command);

        return ResponseEntity.accepted()
                .body(TransferStockWebMapper.toResponse(result));
    }
}
