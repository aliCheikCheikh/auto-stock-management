package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.category.DuplicateCategoryNameException;
import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;

import java.util.Objects;

/**
 * Creates a category after checking name uniqueness. The database constraint protects against
 * concurrent duplicates.
 */
public class CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public CreateCategoryUseCase(CategoryRepository categoryRepository, TransactionRunner transactionRunner) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner cannot be null");
    }

    public Category create(String name) {
        return transactionRunner.execute(() -> {
            // Check the normalized name that the aggregate will store.
            Category category = Category.create(CategoryId.generate(), name);

            if (categoryRepository.existsByName(category.getName())) {
                throw new DuplicateCategoryNameException(category.getName());
            }

            categoryRepository.save(category);
            return category;
        });
    }
}
