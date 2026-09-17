package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.RecordPaymentCommand;
import com.aliCheikh.stock.application.dto.RecordPaymentResult;
import com.aliCheikh.stock.application.port.TransactionRunner;
import com.aliCheikh.stock.domain.exception.sale.SaleNotFoundException;
import com.aliCheikh.stock.domain.model.sale.Payment;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.port.SaleRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Records a payment with exclusive access to the sale, preventing concurrent payments from
 * exceeding the balance. The aggregate validates the payment amount and settlement status.
 */
public class RecordPaymentUseCase {

    private final SaleRepository saleRepository;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public RecordPaymentUseCase(SaleRepository saleRepository,
                                TransactionRunner transactionRunner,
                                Clock clock) {
        this.saleRepository = Objects.requireNonNull(saleRepository, "saleRepository cannot be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public RecordPaymentResult record(RecordPaymentCommand command) {
        Objects.requireNonNull(command, "command cannot be null");
        return transactionRunner.execute(() -> doRecord(command));
    }

    private RecordPaymentResult doRecord(RecordPaymentCommand command) {
        Sale sale = saleRepository.findByIdForUpdate(command.saleId())
                .orElseThrow(() -> new SaleNotFoundException(command.saleId()));

        Payment payment = sale.recordPayment(
                command.amount(),
                command.receivedBy(),
                LocalDateTime.now(clock));

        saleRepository.save(sale);

        return new RecordPaymentResult(
                sale.getSaleId(),
                payment.getAmount(),
                payment.getReceivedAt(),
                sale.getTotalAmount(),
                sale.getAmountPaid(),
                sale.getAmountDue(),
                !sale.isOnCredit());
    }
}
