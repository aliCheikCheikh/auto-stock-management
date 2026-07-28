package com.aliCheikh.stock.domain.exception.customer;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;

/**
 * Levée quand un numéro de téléphone est déjà rattaché à un autre client.
 *
 * <p>Le téléphone est l'identifiant naturel du client : deux fiches partageant le même numéro
 * rendraient le suivi des créances ambigu.</p>
 */
public class DuplicatePhoneNumberException extends DomainException {

    private final PhoneNumber duplicatedPhoneNumber;

    public DuplicatePhoneNumberException(PhoneNumber duplicatedPhoneNumber) {
        super("A customer already exists with the phone number " + duplicatedPhoneNumber);
        this.duplicatedPhoneNumber = duplicatedPhoneNumber;
    }

    public PhoneNumber getDuplicatedPhoneNumber() {
        return duplicatedPhoneNumber;
    }
}
