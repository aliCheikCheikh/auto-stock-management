package com.aliCheikh.stock.domain.exception.category;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.category.CategoryId;

/** Levée quand une catégorie référencée n'existe pas. */
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
