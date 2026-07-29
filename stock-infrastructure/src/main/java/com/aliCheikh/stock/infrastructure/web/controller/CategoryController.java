package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.usecase.CreateCategoryUseCase;
import com.aliCheikh.stock.application.usecase.DeleteCategoryUseCase;
import com.aliCheikh.stock.application.usecase.ListCategoriesUseCase;
import com.aliCheikh.stock.application.usecase.RenameCategoryUseCase;
import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.infrastructure.web.dto.CategoryRequest;
import com.aliCheikh.stock.infrastructure.web.dto.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Familles de pièces.
 *
 * <p>La lecture reste ouverte à tout utilisateur authentifié — le vendeur en a besoin pour classer
 * un produit — tandis que la création, le renommage et la suppression relèvent du patron.</p>
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final RenameCategoryUseCase renameCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public CategoryController(ListCategoriesUseCase listCategoriesUseCase,
                              CreateCategoryUseCase createCategoryUseCase,
                              RenameCategoryUseCase renameCategoryUseCase,
                              DeleteCategoryUseCase deleteCategoryUseCase) {
        this.listCategoriesUseCase = Objects.requireNonNull(listCategoriesUseCase);
        this.createCategoryUseCase = Objects.requireNonNull(createCategoryUseCase);
        this.renameCategoryUseCase = Objects.requireNonNull(renameCategoryUseCase);
        this.deleteCategoryUseCase = Objects.requireNonNull(deleteCategoryUseCase);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> responses = listCategoriesUseCase.execute()
                .stream()
                .map(view -> new CategoryResponse(view.categoryId().getValue(), view.name()))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category created = createCategoryUseCase.create(request.name());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse renameCategory(@PathVariable UUID categoryId,
                                           @Valid @RequestBody CategoryRequest request) {
        return toResponse(renameCategoryUseCase.rename(CategoryId.of(categoryId), request.name()));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID categoryId) {
        deleteCategoryUseCase.delete(CategoryId.of(categoryId));

        return ResponseEntity.noContent().build();
    }

    private static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getCategoryId().getValue(), category.getName());
    }
}
