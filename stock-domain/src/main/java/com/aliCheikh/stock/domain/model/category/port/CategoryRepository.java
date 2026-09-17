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

    /** Checks whether the category name exists, ignoring case. */
    boolean existsByName(String name);

    /** Checks name uniqueness while excluding the category being renamed. */
    boolean existsByNameExcluding(String name, CategoryId excludedCategoryId);
}
