package com.aliCheikh.stock.domain.exception.category;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.category.CategoryId;

/** Raised when a referenced category does not exist. */
public class CategoryNotFoundException extends DomainException {

    private final CategoryId categoryId;

    public CategoryNotFoundException(CategoryId categoryId) {
        super("Category not found: " + categoryId);
        this.categoryId = categoryId;
    }

    public CategoryId getCategoryId() {
        return categoryId;
    }
}
