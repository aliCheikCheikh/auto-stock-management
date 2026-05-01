package com.aliCheikh.stock.domain.model.product;

import com.aliCheikh.stock.domain.exception.product.InvalidProductNameException;
import com.aliCheikh.stock.domain.exception.product.InvalidProductPriceException;
import com.aliCheikh.stock.domain.exception.product.InvalidProductReferenceException;
import com.aliCheikh.stock.domain.exception.product.InvalidThresholdException;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.shared.Money;

import java.util.Objects;

public class Product {
    private final ProductId productId; // L'identité est immuable
    private String name;
    private String reference;
    private CategoryId categoryId;
    private int minimumGlobalThreshold;
    private Money unitPrice;

    public Product(ProductId productId, String name, String reference, CategoryId categoryId, int minimumGlobalThreshold, Money unitPrice) {
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
        // Le constructeur délègue la validation aux gardiens
        this.name = validateName(name);
        this.reference = validateReference(reference);
        this.categoryId = validateCategoryId(categoryId);
        this.minimumGlobalThreshold = validateThreshold(minimumGlobalThreshold);
        this.unitPrice = validatePrice(unitPrice);
    }

    public void rename(String newName) {
        this.name = validateName(newName);
    }

    public void updateReference(String newReference) {
        this.reference = validateReference(newReference);
    }

    public void changeCategory(CategoryId newCategoryId) {
        this.categoryId = validateCategoryId(newCategoryId);
    }

    public void updateThreshold(int newThreshold) {
        this.minimumGlobalThreshold = validateThreshold(newThreshold);
    }

    public void updatePrice(Money newPrice) {
        this.unitPrice = validatePrice(newPrice);
    }

    public boolean isGlobalStockBelowThreshold(int globalQuantity) {
        return globalQuantity < minimumGlobalThreshold;
    }


    private String validateName(String nameToValidate) {
        if (nameToValidate == null || nameToValidate.isBlank()) {
            throw new InvalidProductNameException(nameToValidate);
        }
        return nameToValidate;
    }

    private String validateReference(String referenceToValidate) {
        if (referenceToValidate == null || referenceToValidate.isBlank()) {
            throw new InvalidProductReferenceException(referenceToValidate);
        }
        return referenceToValidate;
    }

    private CategoryId validateCategoryId(CategoryId categoryIdToValidate) {
        return Objects.requireNonNull(categoryIdToValidate, "categoryId cannot be null");
    }

    private int validateThreshold(int thresholdToValidate) {
        if (thresholdToValidate < 0) {
            throw new InvalidThresholdException(thresholdToValidate);
        }
        return thresholdToValidate;
    }

    private Money validatePrice(Money priceToValidate) {
        if (priceToValidate == null || !priceToValidate.isPositive()) {
            throw new InvalidProductPriceException(priceToValidate);
        }
        return priceToValidate;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    public CategoryId getCategoryId() {
        return categoryId;
    }

    public int getMinimumGlobalThreshold() {
        return minimumGlobalThreshold;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }
}