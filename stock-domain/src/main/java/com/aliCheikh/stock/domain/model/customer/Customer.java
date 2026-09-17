package com.aliCheikh.stock.domain.model.customer;

import com.aliCheikh.stock.domain.exception.customer.InvalidCustomerEmailException;
import com.aliCheikh.stock.domain.exception.customer.InvalidCustomerGivenNameException;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Customer aggregate. The shop identifies customers by a required given name and optional father
 * name, rather than a first/last-name pair. Equality uses {@link CustomerId}; {@link PhoneNumber}
 * is the required natural identifier. Email is optional. Uniqueness is checked through the
 * repository and enforced by database constraints.
 */
public final class Customer {

    /** Basic email syntax validation, not full RFC 5322 validation. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CustomerId customerId;
    private final PhoneNumber phoneNumber;
    private final String givenName;
    private final String fatherName;
    private final String email;

    /** Validates invariants shared by all construction paths. */
    private Customer(CustomerId customerId,
                     PhoneNumber phoneNumber,
                     String givenName,
                     String fatherName,
                     String email) {
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber cannot be null");
        this.givenName = requireUsableGivenName(givenName);
        this.fatherName = normalizeOptionalText(fatherName);
        this.email = normalizeOptionalEmail(email);
    }

    /**
     * Creates a customer with a required ID, phone number and given name. Father name and email
     * are optional; a supplied email is validated.
     */
    public static Customer create(CustomerId customerId,
                                  PhoneNumber phoneNumber,
                                  String givenName,
                                  String fatherName,
                                  String email) {
        return new Customer(customerId, phoneNumber, givenName, fatherName, email);
    }

    private static String requireUsableGivenName(String givenName) {
        if (givenName == null || givenName.isBlank()) {
            throw new InvalidCustomerGivenNameException(givenName);
        }
        return givenName.trim();
    }

    /** Represent absent or blank optional text as null. */
    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /** Normalize email case and whitespace before uniqueness checks. */
    private static String normalizeOptionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new InvalidCustomerEmailException(email);
        }
        return normalizedEmail;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    /** Required customer given name. */
    public String getGivenName() {
        return givenName;
    }

    /** Optional father name. */
    public Optional<String> getFatherName() {
        return Optional.ofNullable(fatherName);
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    /** Customer equality depends on identity, not mutable details. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Customer other = (Customer) o;
        return customerId.equals(other.customerId);
    }

    @Override
    public int hashCode() {
        return customerId.hashCode();
    }

    @Override
    public String toString() {
        return "Customer{" + customerId + ", " + givenName + ", " + phoneNumber + "}";
    }
}
