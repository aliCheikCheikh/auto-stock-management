package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.OutstandingDebtView;
import com.aliCheikh.stock.application.port.OutstandingDebtQueryPort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Consultation des créances en cours, globalement ou pour un client. */
public class ListOutstandingDebtsUseCase {

    private final OutstandingDebtQueryPort outstandingDebtQueryPort;

    public ListOutstandingDebtsUseCase(OutstandingDebtQueryPort outstandingDebtQueryPort) {
        this.outstandingDebtQueryPort = Objects.requireNonNull(
                outstandingDebtQueryPort,
                "outstandingDebtQueryPort cannot be null");
    }

    public List<OutstandingDebtView> listAll() {
        return outstandingDebtQueryPort.findAllOutstanding();
    }

    public List<OutstandingDebtView> listByCustomer(UUID customerId) {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        return outstandingDebtQueryPort.findOutstandingByCustomer(customerId);
    }
}
