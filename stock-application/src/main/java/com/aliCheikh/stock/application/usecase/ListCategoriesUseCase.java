package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.CategoryView;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;

import java.util.List;
import java.util.Objects;

public class ListCategoriesUseCase {
    private final CategoryRepository categoryRepository;

    public ListCategoriesUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
    }

    public List<CategoryView> execute() {
        return categoryRepository
                .findAll()
                .stream()
                .map(category -> new CategoryView(category.getCategoryId(), category.getName()))
                .toList();
    }
}
