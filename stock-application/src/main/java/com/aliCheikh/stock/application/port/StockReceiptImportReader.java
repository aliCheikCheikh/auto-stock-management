package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.StockReceiptImportFile;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowData;

import java.util.List;

public interface StockReceiptImportReader {
    List<StockReceiptImportRowData> read(StockReceiptImportFile file);
}
