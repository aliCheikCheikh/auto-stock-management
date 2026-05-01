package com.aliCheikh.stock.domain.exception.category;

import com.aliCheikh.stock.domain.exception.DomainException;

public class InvalidCategoryNameException extends DomainException {
    // Le constructeur prend la valeur invalide, pas le message !
    public InvalidCategoryNameException(String invalidName) {
        // On formate un message hyper clair pour les logs
        super("Category name cannot be null or blank. Provided value: '" + invalidName + "'");
    }
}