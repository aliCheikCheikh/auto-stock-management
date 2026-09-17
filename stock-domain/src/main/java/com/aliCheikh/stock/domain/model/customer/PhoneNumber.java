package com.aliCheikh.stock.domain.model.customer;

import com.aliCheikh.stock.domain.exception.customer.InvalidPhoneNumberException;

/**
 * Chadian phone number in canonical E.164 form: {@code +235XXXXXXXX}. {@link #of(String)}
 * normalizes common input formats and validates the national number length.
 */
public final class PhoneNumber {

    /** Chad country code without the leading plus sign. */
    private static final String CHAD_COUNTRY_CODE = "235";

    /** National phone number length after removing prefixes. */
    private static final int NATIONAL_NUMBER_LENGTH = 8;

    /** International dialing prefix, equivalent to a leading plus sign. */
    private static final String INTERNATIONAL_CALL_PREFIX = "00";

    /** Separators and other non-digit characters removed during normalization. */
    private static final String NON_DIGIT_PATTERN = "\\D";

    private final String value;

    private PhoneNumber(String value) {
        this.value = value;
    }

    /**
     * Creates a canonical phone number from user input. Throws {@link InvalidPhoneNumberException}
     * for blank input or an invalid national number length.
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
     * Remove punctuation before stripping international prefixes, then validate the national
     * number length.
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

    /** Canonical phone number in {@code +235XXXXXXXX} form. */
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
