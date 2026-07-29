package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.category.CategoryNotFoundException;
import com.aliCheikh.stock.domain.exception.category.DuplicateCategoryNameException;
import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;

import java.util.Objects;

/**
 * Renomme une famille de pièces.
 *
 * <p>Les produits référencent leur catégorie par identité : le renommage ne rompt aucune
 * association et n'exige aucune reprise de données.</p>
 */
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

            // La catégorie elle-même est exclue du contrôle : corriger la casse de son propre nom
            // ne doit pas être refusé comme un doublon.
            if (categoryRepository.existsByNameExcluding(category.getName(), categoryId)) {
                throw new DuplicateCategoryNameException(category.getName());
            }

            categoryRepository.save(category);
            return category;
        });
    }
}
