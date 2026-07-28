package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;

/**
 * Levée quand une saisie ne peut pas être interprétée comme un numéro de téléphone exploitable.
 *
 * <p>Le message porte à la fois la saisie fautive et la raison du rejet, pour qu'un log suffise
 * au diagnostic sans avoir à relire le code.</p>
 */
public class InvalidPhoneNumberException extends DomainException {

    private final String rawPhoneNumber;

    public InvalidPhoneNumberException(String rawPhoneNumber, String reason) {
        super("Invalid phone number \"" + rawPhoneNumber + "\": " + reason);
        this.rawPhoneNumber = rawPhoneNumber;
    }

    /** La saisie d'origine, telle que fournie par l'utilisateur. */
    public String getRawPhoneNumber() {
        return rawPhoneNumber;
    }
}
