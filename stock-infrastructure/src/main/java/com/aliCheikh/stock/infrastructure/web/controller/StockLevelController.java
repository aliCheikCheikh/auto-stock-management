package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.usecase.ListStockLevelsUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfStockLevelResponse;

import com.aliCheikh.stock.infrastructure.web.mapper.StockLevelWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock-levels")
public class StockLevelController {

    private ListStockLevelsUseCase listStockLevelsUseCase;

    public StockLevelController(ListStockLevelsUseCase listStockLevelsUseCase) {
        this.listStockLevelsUseCase = listStockLevelsUseCase;
    }


    @GetMapping
    public ResponseEntity<PageOfStockLevelResponse> listStockLevels(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
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
}
