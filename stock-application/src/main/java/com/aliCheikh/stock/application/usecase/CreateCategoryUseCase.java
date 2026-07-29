package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.category.DuplicateCategoryNameException;
import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;

import java.util.Objects;

/**
 * Crée une famille de pièces.
 *
 * <p>L'unicité du nom ne peut pas être portée par l'agrégat, qui ne voit pas les autres catégories :
 * elle est vérifiée ici pour renvoyer une erreur métier explicite plutôt qu'une violation de
 * contrainte SQL. L'index en base reste le garde-fou en cas de création concurrente.</p>
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
            // L'agrégat normalise le nom : on interroge la forme qui sera réellement stockée.
            Category category = Category.create(CategoryId.generate(), name);

            if (categoryRepository.existsByName(category.getName())) {
                throw new DuplicateCategoryNameException(category.getName());
            }

            categoryRepository.save(category);
            return category;
        });
    }
}
