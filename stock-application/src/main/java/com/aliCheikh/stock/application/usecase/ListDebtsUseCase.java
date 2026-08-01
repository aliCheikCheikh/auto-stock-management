package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.DebtSummary;
import com.aliCheikh.stock.application.dto.DebtView;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.port.DebtQueryPort;
import com.aliCheikh.stock.domain.service.CreditPolicy;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Consultation des créances, en cours ou éteintes, globalement ou pour un client.
 *
 * <p>C'est ici qu'on applique la politique de crédit du domaine, de sorte que tous les canaux —
 * écran, export, future relance — partagent la même règle plutôt que d'en recopier chacun sa
 * version.</p>
 */
public class ListDebtsUseCase {

    private final DebtQueryPort debtQueryPort;
    private final Clock clock;

    public ListDebtsUseCase(DebtQueryPort debtQueryPort, Clock clock) {
        this.debtQueryPort = Objects.requireNonNull(
                debtQueryPort, "debtQueryPort cannot be null");
        // Injectée plutôt que LocalDateTime.now() en dur : l'ancienneté devient testable.
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public PageResult<DebtSummary> execute(ListDebtsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        PageResult<DebtView> page = debtQueryPort.findByQuery(query);
        LocalDateTime now = LocalDateTime.now(clock);

        return new PageResult<>(
                page.content().stream().map(debt -> summarize(debt, now)).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    /**
     * Une créance éteinte cesse de vieillir au jour de son règlement.
     *
     * <p>Continuer à compter jusqu'à aujourd'hui afficherait « 180 jours, en retard » sur une
     * dette réglée en une semaine. Le patron lirait un retard là où il n'y a qu'un dossier ancien,
     * et cesserait de faire confiance à l'écran.</p>
     */
    private static DebtSummary summarize(DebtView debt, LocalDateTime now) {
        // Le solde fait foi, et non la présence d'un paiement : une vente peut être soldée d'un
        // seul versement au comptoir comme de cinq remboursements successifs.
        boolean settled = !debt.amountDue().isPositive();

        // Le total d'une vente est strictement positif : une vente soldée porte donc forcément au
        // moins un encaissement, et sa date de règlement ne peut pas manquer.
        LocalDateTime settledAt = settled ? debt.lastPaymentAt() : null;
        LocalDateTime reference = settled ? settledAt : now;

        return new DebtSummary(
                debt.saleId(),
                debt.occurredAt(),
                debt.customerId(),
                debt.customerGivenName(),
                debt.customerFatherName(),
                debt.customerPhoneNumber(),
                debt.totalAmount(),
                debt.amountPaid(),
                debt.amountDue(),
                settled,
                settledAt,
                settled
                        ? CreditPolicy.daysToSettle(debt.occurredAt(), reference)
                        : CreditPolicy.daysOutstanding(debt.occurredAt(), reference),
                settled
                        ? CreditPolicy.wasSettledLate(debt.occurredAt(), reference)
                        : CreditPolicy.isOverdue(debt.occurredAt(), reference));
    }
}
