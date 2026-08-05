package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.StockReceiptImportCategoryCandidate;

import java.util.List;
import java.util.Set;

public interface StockReceiptImportCategoryQueryPort {
    List<StockReceiptImportCategoryCandidate> findByNames(Set<String> normalizedNames);
}
