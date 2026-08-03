package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une créance enrichie de la politique de crédit, telle que présentée au patron.
 *
 * <p>{@code overdue} traduit une règle du domaine : l'interface se contente de l'afficher, elle ne
 * décide pas de ce qui constitue un retard.</p>
 *
 * @param settled         {@code true} si plus rien n'est dû
 * @param settledAt       date du règlement, {@code null} tant que la créance est ouverte
 * @param daysOutstanding jours pendant lesquels la créance est restée ouverte : jusqu'à
 *                        aujourd'hui si elle l'est encore, jusqu'au règlement sinon. Une dette
 *                        éteinte ne vieillit plus, et afficher son âge courant ferait passer pour
 *                        un retard ce qui est un dossier clos.
 * @param overdue         dépassement du délai toléré, mesuré sur cette même durée : « en retard »
 *                        pour une créance ouverte, « réglée hors délai » pour une créance éteinte
 */
public record DebtSummary(UUID saleId,
                          LocalDateTime occurredAt,
                          UUID customerId,
                          String customerGivenName,
                          String customerFatherName,
                          String customerPhoneNumber,
                          Money totalAmount,
                          Money amountPaid,
                          Money amountDue,
                          boolean settled,
                          LocalDateTime settledAt,
                          long daysOutstanding,
                          boolean overdue) {
}
