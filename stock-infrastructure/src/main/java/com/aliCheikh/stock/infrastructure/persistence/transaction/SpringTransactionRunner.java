package com.aliCheikh.stock.infrastructure.persistence.transaction;

import com.aliCheikh.stock.application.port.TransactionRunner;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class SpringTransactionRunner implements TransactionRunner {
    @Override
    @Transactional
    public <T> T execute(Supplier<T> work) {
        return work.get();
    }
}
