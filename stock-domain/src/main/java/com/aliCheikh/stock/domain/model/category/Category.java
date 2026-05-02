package com.aliCheikh.stock.domain.model.category;

import com.aliCheikh.stock.domain.exception.category.InvalidCategoryNameException;

import java.util.Objects;

public class Category {
    private final CategoryId categoryId;
    private String name;

    public Category(CategoryId categoryId, String name) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId cannot be null");
        this.name = validateName(name); // On utilise la méthode de validation
    }

    public void rename(String newName) {
        this.name = validateName(newName); // Le même invariant est protégé ici !
    }

    // Le gardien de l'invariant centralisé
    private String validateName(String nameToValidate) {
        if (nameToValidate == null || nameToValidate.isBlank()) {
            throw new InvalidCategoryNameException(nameToValidate);
        }
        return nameToValidate;
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
        return Objects.hash(categoryId);
    }
}