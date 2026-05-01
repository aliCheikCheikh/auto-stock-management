package com.aliCheikh.stock.domain.exception.product;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.product.ProductId;

public class ProductNotFoundException extends DomainException {

    private final ProductId productId;
    private final String reference;

    // Constructeur historique (recherche par ID)
    public ProductNotFoundException(ProductId productId) {
        super("Produit introuvable dans le catalogue. Aucun produit avec l'ID : '" + productId + "'");
        this.productId = productId;
        this.reference = null; // Indispensable car l'attribut est 'final'
    }

    // Nouveau constructeur (recherche par Référence)
    public ProductNotFoundException(String reference) {
        super("Le produit avec la référence '" + reference + "' est introuvable dans le catalogue.");
        this.productId = null; // Indispensable car l'attribut est 'final'
        this.reference = reference;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getReference() {
        return reference;
    }
}