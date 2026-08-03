package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une vente à crédit, en cours ou éteinte, vue depuis le comptoir.
 *
 * <p>Modèle de lecture volontairement plat, dénormalisé avec les informations du client, pour que
 * l'écran « qui me doit de l'argent » se rende sans requête supplémentaire.</p>
 *
 * <p>Ce type ne porte que des <b>faits</b> : montants, dates, identités. Ce qui s'en déduit —
 * l'ancienneté, le retard, la date de solde — relève de la politique de crédit et se calcule dans
 * le use case, une fois pour tous les canaux.</p>
 *
 * @param lastPaymentAt date du dernier encaissement, {@code null} si le client n'a jamais rien
 *                      versé. Sur une créance éteinte, c'est la date du règlement.
 */
public record DebtView(UUID saleId,
                       LocalDateTime occurredAt,
                       UUID customerId,
                       String customerGivenName,
                       String customerFatherName,
                       String customerPhoneNumber,
                       Money totalAmount,
                       Money amountPaid,
                       Money amountDue,
                       LocalDateTime lastPaymentAt) {
}
