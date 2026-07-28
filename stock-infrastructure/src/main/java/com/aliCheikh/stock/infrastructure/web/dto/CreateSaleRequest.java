package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Demande de vente.
 *
 * <p>{@code customerId} et {@code amountPaid} sont facultatifs : absents, la vente est au comptant.
 * Un {@code amountPaid} inférieur au total crée une créance et exige alors un client — règle
 * vérifiée par le domaine, pas ici.</p>
 */
public record CreateSaleRequest(UUID shopId,
                                @Valid @NotEmpty List<CreateSaleLine> lines,
                                UUID customerId,
                                @PositiveOrZero BigDecimal amountPaid) {
}
