package com.aliCheikh.stock.infrastructure.web.controller;


import com.aliCheikh.stock.application.usecase.ListCategoriesUseCase;
import com.aliCheikh.stock.infrastructure.web.dto.CategoryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final ListCategoriesUseCase listCategoriesUseCase;


    public CategoryController(ListCategoriesUseCase listCategoriesUseCase) {
        this.listCategoriesUseCase = listCategoriesUseCase;

    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> responses = listCategoriesUseCase.execute()
                .stream()
                .map(view -> new CategoryResponse(view.categoryId().getValue(), view.name()))
                .toList();
        return ResponseEntity.ok(responses);

    }


}
