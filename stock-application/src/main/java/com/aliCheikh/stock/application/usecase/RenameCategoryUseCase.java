package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.category.CategoryNotFoundException;
import com.aliCheikh.stock.domain.exception.category.DuplicateCategoryNameException;
import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;

import java.util.Objects;

/** Renames a category while preserving product associations through its stable ID. */
public class RenameCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final TransactionRunner transactionRunner;

    public RenameCategoryUseCase(CategoryRepository categoryRepository, TransactionRunner transactionRunner) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner cannot be null");
    }

    public Category rename(CategoryId categoryId, String newName) {
        Objects.requireNonNull(categoryId, "categoryId cannot be null");

        return transactionRunner.execute(() -> {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CategoryNotFoundException(categoryId));

            category.rename(newName);

            // Exclude this category so a case-only rename is not treated as a duplicate.
            if (categoryRepository.existsByNameExcluding(category.getName(), categoryId)) {
                throw new DuplicateCategoryNameException(category.getName());
            }

            categoryRepository.save(category);
            return category;
        });
    }
}
