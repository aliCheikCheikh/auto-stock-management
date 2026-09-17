package com.aliCheikh.stock.domain.model.product;

import com.aliCheikh.stock.domain.exception.product.*;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.shared.Money;

import java.util.Objects;

public class Product {
    private final ProductId productId;
    private String name;
    private final String reference;
    private final CategoryId categoryId;
    private int minimumGlobalThreshold;
    private Money unitPrice;
    private boolean active;

    public Product(ProductId productId, String name, String reference, CategoryId categoryId, int minimumGlobalThreshold, Money unitPrice) {
        this(productId, name, reference, categoryId, minimumGlobalThreshold, unitPrice, true);
    }

    private Product(ProductId productId,
                    String name,
                    String reference,
                    CategoryId categoryId,
                    int minimumGlobalThreshold,
                    Money unitPrice,
                    boolean active) {
        this.productId = Objects.requireNonNull(productId, "productId cannot be null");
        this.name = validateName(name);
        this.reference = validateReference(reference);
        this.categoryId = validateCategoryId(categoryId);
        this.minimumGlobalThreshold = validateThreshold(minimumGlobalThreshold);
        this.unitPrice = validatePrice(unitPrice);
        this.active = active;
    }

    public static Product restore(ProductId productId,
                                  String name,
                                  String reference,
                                  CategoryId categoryId,
                                  int minimumGlobalThreshold,
                                  Money unitPrice,
                                  boolean active
    ) {
        return new Product(productId, name, reference, categoryId, minimumGlobalThreshold, unitPrice, active);
    }

    public void rename(String newName) {
        this.name = validateName(newName);
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

    public void deactivate() {
        this.active = false;
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

    public boolean isActive() {
        return this.active;
    }

    public void ensureActive() {
        if (!isActive()) {
            throw new InactiveProductException(this.productId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Product product = (Product) o;
        return productId.equals(product.getProductId());
    }

    @Override
    public int hashCode() {
        return productId.hashCode();
    }

}
