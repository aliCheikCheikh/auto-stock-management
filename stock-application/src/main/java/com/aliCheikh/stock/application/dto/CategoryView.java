package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.category.CategoryId;

public record CategoryView(CategoryId categoryId,
                           String name) {
}
