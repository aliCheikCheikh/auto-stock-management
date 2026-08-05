package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.StockReceiptImportProductCandidate;

import java.util.List;
import java.util.Set;

public interface StockReceiptImportProductQueryPort {
    List<StockReceiptImportProductCandidate> findCandidates(
            Set<String> normalizedReferences,
            Set<String> normalizedNames
    );
}
