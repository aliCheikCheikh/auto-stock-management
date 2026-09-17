package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.CustomerSearchView;

import java.util.List;

/** Database-backed customer search returning a flat read model for the checkout selector. */
public interface CustomerSearchQueryPort {

    /**
     * Matches the customer's name, father name or phone number. {@code keyword} is normalized by
     * the use case; {@code limit} bounds the results.
     */
    List<CustomerSearchView> findCustomersByKeyword(String keyword, int limit);

    /** Most recently registered customers, used before a search term is entered. */
    List<CustomerSearchView> findMostRecent(int limit);
}
