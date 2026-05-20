package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.usecase.ListStockLevelsUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfStockLevelResponse;

import com.aliCheikh.stock.infrastructure.web.mapper.StockLevelWebMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-levels")
@Validated
public class StockLevelController {

    private final ListStockLevelsUseCase listStockLevelsUseCase;

    public StockLevelController(ListStockLevelsUseCase listStockLevelsUseCase) {
        this.listStockLevelsUseCase = listStockLevelsUseCase;
    }


    @GetMapping
    public ResponseEntity<PageOfStockLevelResponse> listStockLevels(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
                                                                    @RequestParam(required = false) UUID productId,
                                                                    @RequestParam(required = false) UUID shopId,
                                                                    @RequestParam(required = false) UUID locationId,
                                                                    @RequestParam(defaultValue = "false") boolean belowThreshold) {
        ListStockLevelsQuery query = StockLevelWebMapper.toQuery(page,
                size,
                productId,
                shopId,
                locationId,
                belowThreshold);

        PageResult<StockLevelView> pageResult = listStockLevelsUseCase.execute(query);
        PageOfStockLevelResponse response = StockLevelWebMapper.toPageResponse(pageResult);
        return ResponseEntity.ok(response);

    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Void> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.badRequest().build();
    }
}
