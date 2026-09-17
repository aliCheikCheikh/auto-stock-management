package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Sale request. Null {@code customerId} and {@code amountPaid} mean a fully paid cash sale. A
 * payment below the total requires a customer, as enforced by {@code Sale}.
 */
public record SellProductCommand(UserId sellerId,
                                 ShopId shopId,
                                 List<SellLineCommand> lines,
                                 CustomerId customerId,
                                 Money amountPaid) {

    public SellProductCommand {
        Objects.requireNonNull(sellerId, "sellerId cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");
        Objects.requireNonNull(lines, "lines cannot be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("at least one line is required");
        }
    }

    /** Convenience constructor for a fully paid cash sale. */
    public SellProductCommand(UserId sellerId, ShopId shopId, List<SellLineCommand> lines) {
        this(sellerId, shopId, lines, null, null);
    }
}
