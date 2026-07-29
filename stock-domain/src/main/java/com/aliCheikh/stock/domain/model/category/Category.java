package com.aliCheikh.stock.domain.model.category;

import com.aliCheikh.stock.domain.exception.category.InvalidCategoryNameException;

import java.util.Objects;

/**
 * Famille de pièces sous laquelle le magasin range ses produits.
 *
 * <p>Le nom est normalisé (espaces de bordure retirés) dès la construction : sans forme canonique,
 * « Freinage » et « Freinage  » cohabiteraient et l'unicité ne voudrait plus rien dire.</p>
 */
public class Category {

    private final CategoryId categoryId;
    private String name;

    public Category(CategoryId categoryId, String name) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId cannot be null");
        this.name = requireUsableName(name);
    }

    /** Nouvelle catégorie créée par le patron. */
    public static Category create(CategoryId categoryId, String name) {
        return new Category(categoryId, name);
    }

    /**
     * Renomme la catégorie.
     *
     * <p>Les produits la référencent par identité, jamais par son nom : un renommage ne rompt donc
     * aucune association.</p>
     */
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
