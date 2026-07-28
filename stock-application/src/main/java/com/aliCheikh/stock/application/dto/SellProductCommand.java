package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.util.List;
import java.util.Objects;

/**
 * Demande de vente.
 *
 * <p>{@code customerId} et {@code amountPaid} sont facultatifs et portent la vente à crédit :</p>
 * <ul>
 *     <li>tous deux {@code null} → vente au comptant, intégralement payée, sans client ;</li>
 *     <li>{@code amountPaid} renseigné et inférieur au total → créance, et {@code customerId}
 *         devient obligatoire (règle portée par l'agrégat {@code Sale}).</li>
 * </ul>
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

    /** Vente au comptant : conserve la signature antérieure aux créances. */
    public SellProductCommand(UserId sellerId, ShopId shopId, List<SellLineCommand> lines) {
        this(sellerId, shopId, lines, null, null);
    }
}
