package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Résultat d'une vente.
 *
 * <p>{@code amountDue} est renvoyé au vendeur pour qu'il sache immédiatement ce que le client
 * reste devoir — c'est l'information qu'il annonce de vive voix au comptoir.</p>
 */
public record SellProductResult(
        SaleId saleId,
        UserId sellerId,
        List<SaleLineDto> lines,
        Money totalAmount,
        LocalDateTime createdAt,
        Optional<CustomerId> customerId,
        Money amountPaid,
        Money amountDue
) {

    /** Vente au comptant : intégralement payée, sans client, donc rien à devoir. */
    public SellProductResult(SaleId saleId,
                             UserId sellerId,
                             List<SaleLineDto> lines,
                             Money totalAmount,
                             LocalDateTime createdAt) {
        this(saleId, sellerId, lines, totalAmount, createdAt,
                Optional.empty(),
                totalAmount,
                totalAmount.subtract(totalAmount));
    }
}
