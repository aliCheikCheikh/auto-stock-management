package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ListStockMovementsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockMovementView;
import com.aliCheikh.stock.application.usecase.ListStockMovementsUseCase;
import com.aliCheikh.stock.domain.model.movement.MovementType;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfStockMovementResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.StockMovementWebMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-movements")
@Validated
public class StockMovementController {

    private final ListStockMovementsUseCase listStockMovementsUseCase;

    public StockMovementController(ListStockMovementsUseCase listStockMovementsUseCase) {
        this.listStockMovementsUseCase = listStockMovementsUseCase;
    }

    @GetMapping
    public ResponseEntity<PageOfStockMovementResponse> listStockMovements(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam MultiValueMap<String, String> queryParams
    ) {
        ListStockMovementsQuery query = StockMovementWebMapper.toQuery(
                page,
                size,
                queryParams.get("sort"),
                productId,
                locationId,
                type,
                from,
                to
        );
        PageResult<StockMovementView> result = listStockMovementsUseCase.execute(query);

        return ResponseEntity.ok(StockMovementWebMapper.toPageResponse(result));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Void> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.badRequest().build();
    }
}
