package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.CustomerSearchView;
import com.aliCheikh.stock.application.port.CustomerSearchQueryPort;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;

import java.util.List;
import java.util.Objects;

/** Searches customers by name or phone; returns recent customers when no keyword is supplied. */
public class SearchCustomersUseCase {

    private static final int MAX_RESULTS = 10;
    private static final int MIN_KEYWORD_LENGTH = 2;

    private final CustomerSearchQueryPort customerSearchQueryPort;

    public SearchCustomersUseCase(CustomerSearchQueryPort customerSearchQueryPort) {
        this.customerSearchQueryPort = Objects.requireNonNull(
                customerSearchQueryPort, "customerSearchQueryPort cannot be null");
    }

    public List<CustomerSearchView> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerSearchQueryPort.findMostRecent(MAX_RESULTS);
        }

        String trimmed = keyword.trim();
        if (trimmed.length() < MIN_KEYWORD_LENGTH) {
            return List.of();
        }

        return customerSearchQueryPort.findCustomersByKeyword(normalizeIfPhoneNumber(trimmed), MAX_RESULTS);
    }

    /**
     * Normalize complete phone numbers; fall back to the raw keyword for names and partial phone
     * numbers.
     */
    private static String normalizeIfPhoneNumber(String keyword) {
        try {
            return PhoneNumber.of(keyword).getValue();
        } catch (RuntimeException notAPhoneNumber) {
            return keyword;
        }
    }
}
