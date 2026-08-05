package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.usecase.GetDashboardSummaryUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.DashboardResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.DashboardWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final GetDashboardSummaryUseCase getDashboardSummaryUseCase;

    public DashboardController(GetDashboardSummaryUseCase getDashboardSummaryUseCase) {
        this.getDashboardSummaryUseCase = getDashboardSummaryUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardResponse> summary() {
        return ResponseEntity.ok(DashboardWebMapper.toResponse(
                getDashboardSummaryUseCase.execute()));
    }
}
