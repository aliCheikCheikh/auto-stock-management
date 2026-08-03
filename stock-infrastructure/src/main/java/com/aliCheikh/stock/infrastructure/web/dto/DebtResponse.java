package com.aliCheikh.stock.infrastructure.web.dto;

import com.aliCheikh.stock.application.dto.DebtSummary;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une créance telle qu'affichée dans l'écran « qui me doit de l'argent » et dans son historique.
 *
 * <p>{@code daysOutstanding} et {@code overdue} viennent de la politique de crédit du domaine :
 * l'interface les affiche, elle ne les recalcule pas.</p>
 *
 * @param settled         {@code true} si plus rien n'est dû
 * @param settledAt       date du règlement, {@code null} tant que la créance est ouverte
 * @param daysOutstanding durée d'ouverture de la créance : jusqu'à aujourd'hui si elle est encore
 *                        vivante, jusqu'au règlement sinon
 * @param overdue         « en retard » pour une créance ouverte, « réglée hors délai » pour une
 *                        créance éteinte
 */
public record DebtResponse(UUID saleId,
                           LocalDateTime occurredAt,
                           UUID customerId,
                           String customerGivenName,
                           String customerFatherName,
                           String customerPhoneNumber,
                           MoneyResponse totalAmount,
                           MoneyResponse amountPaid,
                           MoneyResponse amountDue,
                           boolean settled,
                           LocalDateTime settledAt,
                           long daysOutstanding,
                           boolean overdue) {

    public static DebtResponse from(DebtSummary summary) {
        return new DebtResponse(
                summary.saleId(),
                summary.occurredAt(),
                summary.customerId(),
                summary.customerGivenName(),
                summary.customerFatherName(),
                summary.customerPhoneNumber(),
                MoneyResponse.from(summary.totalAmount()),
                MoneyResponse.from(summary.amountPaid()),
                MoneyResponse.from(summary.amountDue()),
                summary.settled(),
                summary.settledAt(),
                summary.daysOutstanding(),
                summary.overdue());
    }
}
