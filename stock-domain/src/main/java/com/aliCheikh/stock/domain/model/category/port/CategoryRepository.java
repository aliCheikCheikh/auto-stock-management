package com.aliCheikh.stock.domain.model.category.port;

import com.aliCheikh.stock.domain.model.category.Category;
import com.aliCheikh.stock.domain.model.category.CategoryId;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    List<Category> findAll();

    Optional<Category> findById(CategoryId categoryId);

    void save(Category category);

    void delete(CategoryId categoryId);

    /**
     * Indique si une catégorie porte déjà ce nom, <b>à la casse près</b>.
     *
     * <p>Une comparaison sensible à la casse laisserait « Freinage » et « freinage » coexister :
     * deux familles pour la base, une seule dans la tête du patron.</p>
     */
    boolean existsByName(String name);

    /** Même question en ignorant une catégorie donnée — nécessaire lors d'un renommage. */
    boolean existsByNameExcluding(String name, CategoryId excludedCategoryId);
}
