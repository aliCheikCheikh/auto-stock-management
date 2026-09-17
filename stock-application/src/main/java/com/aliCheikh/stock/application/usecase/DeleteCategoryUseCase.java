package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.category.CategoryInUseException;
import com.aliCheikh.stock.domain.exception.category.CategoryNotFoundException;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.category.port.CategoryRepository;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;

import java.util.Objects;

/** Deletes an empty category. Products must be reassigned before their category can be deleted. */
public class DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final TransactionRunner transactionRunner;

    public DeleteCategoryUseCase(CategoryRepository categoryRepository,
                                 ProductRepository productRepository,
                                 TransactionRunner transactionRunner) {
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "categoryRepository cannot be null");
        this.productRepository = Objects.requireNonNull(productRepository, "productRepository cannot be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner cannot be null");
    }

    public void delete(CategoryId categoryId) {
        Objects.requireNonNull(categoryId, "categoryId cannot be null");

        transactionRunner.execute(() -> {
            if (categoryRepository.findById(categoryId).isEmpty()) {
                throw new CategoryNotFoundException(categoryId);
            }

            if (productRepository.existsByCategoryId(categoryId)) {
                throw new CategoryInUseException(categoryId);
            }

            categoryRepository.delete(categoryId);
            return null;
        });
    }
}
