package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;

/**
 * Levée quand le nom propre d'un client est absent ou vide.
 *
 * <p>Ce nom est obligatoire : c'est par lui que le vendeur reconnaît le client dans la liste
 * des créances.</p>
 */
public class InvalidCustomerGivenNameException extends DomainException {

    private final String invalidGivenName;

    public InvalidCustomerGivenNameException(String invalidGivenName) {
        super("Invalid customer given name: \"" + invalidGivenName + "\" (the value must not be null or blank)");
        this.invalidGivenName = invalidGivenName;
    }

    public String getInvalidGivenName() {
        return invalidGivenName;
    }
}
