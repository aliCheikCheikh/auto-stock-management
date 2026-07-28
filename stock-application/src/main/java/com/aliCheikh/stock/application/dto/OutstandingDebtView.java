package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une créance : une vente dont le solde n'est pas soldé, vue depuis le comptoir.
 *
 * <p>Modèle de lecture volontairement plat, dénormalisé avec les informations du client, pour que
 * l'écran « qui me doit de l'argent » se rende sans requête supplémentaire.</p>
 */
public record OutstandingDebtView(UUID saleId,
                                  LocalDateTime occurredAt,
                                  UUID customerId,
                                  String customerGivenName,
                                  String customerFatherName,
                                  String customerPhoneNumber,
                                  Money totalAmount,
                                  Money amountPaid,
                                  Money amountDue) {
}
