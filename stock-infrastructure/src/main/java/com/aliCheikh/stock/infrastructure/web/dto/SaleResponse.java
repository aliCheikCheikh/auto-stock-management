package com.aliCheikh.stock.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Vente renvoyée au client HTTP.
 *
 * <p>{@code amountDue} est fourni pour que le front n'ait jamais à recalculer le solde :
 * une seule source de vérité, celle du domaine.</p>
 */
public record SaleResponse(UUID saleId,
                           UUID sellerId,
                           String sellerName,
                           List<SaleLineResponse> lines,
                           MoneyResponse totalAmount,
                           LocalDateTime createdAt,
                           UUID customerId,
                           MoneyResponse amountPaid,
                           MoneyResponse amountDue) {
}
