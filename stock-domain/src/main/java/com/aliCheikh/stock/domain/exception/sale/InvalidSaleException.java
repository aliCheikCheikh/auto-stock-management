package com.aliCheikh.stock.domain.exception.sale;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.user.UserId;

public class InvalidSaleException extends DomainException {

    private final UserId sellerId;
    private final String reason;

    public InvalidSaleException(UserId sellerId, String reason) {
        // Le message est généré automatiquement et proprement pour les logs de production
        super(String.format("Invalid sale operation by seller %s. Reason: %s", sellerId, reason));

        this.sellerId = sellerId;
        this.reason = reason;
    }

    public UserId getSellerId() {
        return sellerId;
    }

    public String getReason() {
        return reason;
    }
}