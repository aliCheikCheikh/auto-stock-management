package com.aliCheikh.stock.domain.model.customer;

import com.aliCheikh.stock.domain.exception.customer.InvalidPhoneNumberException;

/**
 * Numéro de téléphone tchadien, stocké sous forme canonique E.164 : {@code +235XXXXXXXX}.
 *
 * <p>La normalisation est la raison d'être de ce Value Object : deux écritures humaines du même
 * numéro ({@code "66 12 34 56"}, {@code "+235-66-12-34-56"}, {@code "0023566123456"}) produisent
 * le même objet. C'est ce qui rend l'unicité du téléphone fiable, en mémoire comme en base.</p>
 *
 * <p>Le seul point d'entrée est {@link #of(String)} : un {@code PhoneNumber} qui existe est
 * forcément valide et canonique.</p>
 */
public final class PhoneNumber {

    /** Indicatif du Tchad, en chiffres seuls (le {@code +} appartient à l'affichage, pas à la donnée). */
    private static final String CHAD_COUNTRY_CODE = "235";

    /** Longueur du numéro national tchadien, une fois tout préfixe retiré. */
    private static final int NATIONAL_NUMBER_LENGTH = 8;

    /** Préfixe international composé au clavier, équivalent du {@code +}. */
    private static final String INTERNATIONAL_CALL_PREFIX = "00";

    /** Tout ce qui n'est pas un chiffre : espaces, tirets, points, parenthèses, {@code +}. */
    private static final String NON_DIGIT_PATTERN = "\\D";

    private final String value;

    private PhoneNumber(String value) {
        this.value = value;
    }

    /**
     * Construit un numéro canonique à partir d'une saisie libre.
     *
     * @param rawPhoneNumber la saisie de l'utilisateur, dans n'importe quel format usuel
     * @throws InvalidPhoneNumberException si la saisie est vide ou ne contient pas un numéro
     *                                     national de {@value #NATIONAL_NUMBER_LENGTH} chiffres
     */
    public static PhoneNumber of(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            throw new InvalidPhoneNumberException(rawPhoneNumber, "the value must not be null or blank");
        }

        String nationalNumber = extractNationalNumber(rawPhoneNumber);

        if (nationalNumber.length() != NATIONAL_NUMBER_LENGTH) {
            throw new InvalidPhoneNumberException(
                    rawPhoneNumber,
                    "expected " + NATIONAL_NUMBER_LENGTH + " digits once the country code is removed, but got "
                            + nationalNumber.length());
        }

        return new PhoneNumber("+" + CHAD_COUNTRY_CODE + nationalNumber);
    }

    /**
     * Ramène une saisie libre au seul numéro national.
     *
     * <p>L'ordre des trois étapes est structurant : on retire d'abord la ponctuation (sans quoi un
     * {@code +} en tête empêcherait de reconnaître l'indicatif), puis on retire le préfixe, et
     * seulement ensuite l'appelant peut juger de la longueur.</p>
     */
    private static String extractNationalNumber(String rawPhoneNumber) {
        String digits = rawPhoneNumber.replaceAll(NON_DIGIT_PATTERN, "");

        if (digits.startsWith(INTERNATIONAL_CALL_PREFIX + CHAD_COUNTRY_CODE)) {
            return digits.substring((INTERNATIONAL_CALL_PREFIX + CHAD_COUNTRY_CODE).length());
        }
        if (digits.startsWith(CHAD_COUNTRY_CODE)) {
            return digits.substring(CHAD_COUNTRY_CODE.length());
        }
        if (digits.startsWith("0")) {
            return digits.substring(1);
        }
        return digits;
    }

    /** Le numéro sous forme canonique {@code +235XXXXXXXX}. */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhoneNumber other = (PhoneNumber) o;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
