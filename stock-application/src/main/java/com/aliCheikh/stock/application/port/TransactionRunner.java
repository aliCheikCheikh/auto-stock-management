package com.aliCheikh.stock.application.port;

import java.util.function.Supplier;

public interface TransactionRunner {
    <T> T execute(Supplier<T> work);
}
