package com.aliCheikh.stock.infrastructure.logging;

import com.aliCheikh.stock.application.port.StockReceiptImportFailureReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StockReceiptImportFailureLogger implements StockReceiptImportFailureReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StockReceiptImportFailureLogger.class);

    @Override
    public void report(UUID importId, int lineNumber, RuntimeException failure) {
        LOGGER.error("Échec de la ligne {} pour l'import {}", lineNumber, importId, failure);
    }
}
