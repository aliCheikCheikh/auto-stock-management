package com.aliCheikh.stock.domain.exception.category;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.category.CategoryId;

/** Raised when deleting a category that still contains products. */
public class CategoryInUseException extends DomainException {

    private final CategoryId categoryId;

    public CategoryInUseException(CategoryId categoryId) {
        super("Category " + categoryId + " still holds products and cannot be deleted");
        this.categoryId = categoryId;
    }

    public CategoryId getCategoryId() {
        return categoryId;
    }
}
