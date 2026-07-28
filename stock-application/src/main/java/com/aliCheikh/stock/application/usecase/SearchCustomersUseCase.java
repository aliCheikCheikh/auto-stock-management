package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.CustomerSearchView;
import com.aliCheikh.stock.application.port.CustomerSearchQueryPort;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;

import java.util.List;
import java.util.Objects;

/**
 * Recherche d'un client au comptoir, par nom ou par numéro de téléphone.
 *
 * <p>Sans mot-clé, on renvoie les clients les plus récents : le vendeur retrouve ainsi en un
 * geste ceux à qui il vient de faire crédit, sans rien taper.</p>
 */
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
     * Une recherche par téléphone doit fonctionner quel que soit le format tapé : « 66 12 34 56 »
     * doit retrouver le « +23566123456 » stocké. On tente donc la normalisation, et on retombe sur
     * le mot-clé brut si la saisie n'est pas un numéro complet (nom, ou numéro partiel).
     */
    private static String normalizeIfPhoneNumber(String keyword) {
        try {
            return PhoneNumber.of(keyword).getValue();
        } catch (RuntimeException notAPhoneNumber) {
            return keyword;
        }
    }
}
