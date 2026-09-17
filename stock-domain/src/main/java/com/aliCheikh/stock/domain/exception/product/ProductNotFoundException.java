package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.product.ProductId;

public class ProductNotFoundException extends DomainException {

    private final ProductId productId;
    private final String reference;

    // Constructeur historique (recherche par ID)
    public ProductNotFoundException(ProductId productId) {
        super("Product not found in the catalog. No product with ID '" + productId + "'");
        this.productId = productId;
        this.reference = null;
    }

    // Lookup by product reference.
    public ProductNotFoundException(String reference) {
        super("Product with reference '" + reference + "' was not found in the catalog.");
        this.productId = null;
        this.reference = reference;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getReference() {
        return reference;
    }
}
