package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.usecase.GetSessionContextUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.ContextResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.ContextWebMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the current shop and locations without hard-coded frontend identifiers. */
@RestController
@RequestMapping("/api/v1/context")
public class ContextController {

    private final GetSessionContextUseCase getSessionContextUseCase;

    public ContextController(GetSessionContextUseCase getSessionContextUseCase) {
        this.getSessionContextUseCase = getSessionContextUseCase;
    }

    @GetMapping
    public ResponseEntity<ContextResponse> getContext() {
        return ResponseEntity.ok(ContextWebMapper.toResponse(getSessionContextUseCase.execute()));
    }
}
