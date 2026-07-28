package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.OutstandingDebtSummary;
import com.aliCheikh.stock.application.dto.OutstandingDebtView;
import com.aliCheikh.stock.application.port.OutstandingDebtQueryPort;
import com.aliCheikh.stock.domain.service.CreditPolicy;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Consultation des créances en cours, globalement ou pour un client.
 *
 * <p>L'ancienneté et le caractère « en retard » sont calculés ici en appliquant la politique de
 * crédit du domaine, de sorte que tous les canaux (écran, export, future relance) partagent la
 * même règle.</p>
 */
public class ListOutstandingDebtsUseCase {

    private final OutstandingDebtQueryPort outstandingDebtQueryPort;
    private final Clock clock;

    public ListOutstandingDebtsUseCase(OutstandingDebtQueryPort outstandingDebtQueryPort, Clock clock) {
        this.outstandingDebtQueryPort = Objects.requireNonNull(
                outstandingDebtQueryPort, "outstandingDebtQueryPort cannot be null");
        // Injecté plutôt que LocalDateTime.now() en dur : l'ancienneté devient testable.
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public List<OutstandingDebtSummary> listAll() {
        return summarize(outstandingDebtQueryPort.findAllOutstanding());
    }

    public List<OutstandingDebtSummary> listByCustomer(UUID customerId) {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        return summarize(outstandingDebtQueryPort.findOutstandingByCustomer(customerId));
    }

    private List<OutstandingDebtSummary> summarize(List<OutstandingDebtView> debts) {
        LocalDateTime now = LocalDateTime.now(clock);

        return debts.stream()
                .map(debt -> new OutstandingDebtSummary(
                        debt.saleId(),
                        debt.occurredAt(),
                        debt.customerId(),
                        debt.customerGivenName(),
                        debt.customerFatherName(),
                        debt.customerPhoneNumber(),
                        debt.totalAmount(),
                        debt.amountPaid(),
                        debt.amountDue(),
                        CreditPolicy.daysOutstanding(debt.occurredAt(), now),
                        CreditPolicy.isOverdue(debt.occurredAt(), now)))
                .toList();
    }
}
