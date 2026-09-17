package com.aliCheikh.stock.domain.model.category;

import com.aliCheikh.stock.domain.exception.category.InvalidCategoryNameException;

import java.util.Objects;

/** Product category. Names are trimmed at construction before uniqueness checks. */
public class Category {

    private final CategoryId categoryId;
    private String name;

    public Category(CategoryId categoryId, String name) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId cannot be null");
        this.name = requireUsableName(name);
    }

    /** Creates a new category. */
    public static Category create(CategoryId categoryId, String name) {
        return new Category(categoryId, name);
    }

    /** Renames the category while preserving associations through its ID. */
    public void rename(String newName) {
        this.name = requireUsableName(newName);
    }

    private static String requireUsableName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidCategoryNameException(name);
        }
        return name.trim();
    }

    public CategoryId getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Category category = (Category) o;
        return categoryId.equals(category.categoryId);
    }

    @Override
    public int hashCode() {
        return categoryId.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
