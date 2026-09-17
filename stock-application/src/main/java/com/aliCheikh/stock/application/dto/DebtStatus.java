package com.aliCheikh.stock.application.dto;

/** Application-level query filter. {@link #ALL} is a search criterion, not a sale state. */
public enum DebtStatus {

    /** Credit sales with an outstanding balance. */
    OUTSTANDING,

    /** Settled credit sales retained for payment history. */
    SETTLED,

    /** Both outstanding and settled credit sales. */
    ALL
}
