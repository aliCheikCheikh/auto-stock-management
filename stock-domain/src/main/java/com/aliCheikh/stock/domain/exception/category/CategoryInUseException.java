package com.aliCheikh.stock.domain.exception.category;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.category.CategoryId;

/**
 * Levée quand on tente de supprimer une catégorie encore rattachée à des produits.
 *
 * <p>Un produit appartient toujours à une famille : supprimer la catégorie laisserait des produits
 * sans rangement. Le patron doit d'abord les reclasser, ou simplement renommer la catégorie.</p>
 */
public class CategoryInUseException extends DomainException {

    private final CategoryId categoryId;

    public CategoryInUseException(CategoryId categoryId) {
        super("Category " + categoryId + " still holds products and cannot be deleted");
        this.categoryId = categoryId;
    }

    public CategoryId getCategoryId() {
        return categoryId;
    }
}
