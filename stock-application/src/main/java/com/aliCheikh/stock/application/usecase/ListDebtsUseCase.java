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

/** Lists debts and applies the domain credit policy consistently to outstanding and settled sales. */
public class ListDebtsUseCase {

    private final DebtQueryPort debtQueryPort;
    private final Clock clock;

    public ListDebtsUseCase(DebtQueryPort debtQueryPort, Clock clock) {
        this.debtQueryPort = Objects.requireNonNull(
                debtQueryPort, "debtQueryPort cannot be null");
        // Inject the clock to make debt age deterministic in tests.
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

    /** Stops debt aging at the settlement date. */
    private static DebtSummary summarize(DebtView debt, LocalDateTime now) {
        // Use the remaining balance, not the number of payments, to determine settlement.
        boolean settled = !debt.amountDue().isPositive();

        // A sale has a positive total, so a settled sale must have a payment and settlement date.
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
