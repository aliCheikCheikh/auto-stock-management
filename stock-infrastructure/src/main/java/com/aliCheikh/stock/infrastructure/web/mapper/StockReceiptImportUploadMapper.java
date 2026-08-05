package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.StockReceiptImportFile;
import com.aliCheikh.stock.application.exception.InvalidStockReceiptImportFileException;
import com.aliCheikh.stock.application.exception.StockReceiptImportFileErrorCode;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public final class StockReceiptImportUploadMapper {

    public static final long MAX_FILE_SIZE_BYTES = 1024L * 1024L;

    private StockReceiptImportUploadMapper() {
    }

    public static StockReceiptImportFile toFile(MultipartFile upload) {
        if (upload.isEmpty()) {
            throw invalid(StockReceiptImportFileErrorCode.EMPTY_FILE, "Le fichier CSV est vide.");
        }
        if (upload.getSize() > MAX_FILE_SIZE_BYTES) {
            throw invalid(
                    StockReceiptImportFileErrorCode.FILE_TOO_LARGE,
                    "Le fichier dépasse la taille maximale autorisée de 1 Mo."
            );
        }

        String filename = StringUtils.cleanPath(String.valueOf(upload.getOriginalFilename()));
        if (filename.isBlank() || "null".equals(filename) || filename.contains("..")) {
            throw invalid(
                    StockReceiptImportFileErrorCode.UNSUPPORTED_FILE,
                    "Le nom du fichier CSV est invalide."
            );
        }

        try {
            return new StockReceiptImportFile(filename, upload.getBytes());
        } catch (IOException exception) {
            throw new InvalidStockReceiptImportFileException(
                    StockReceiptImportFileErrorCode.MALFORMED_CSV,
                    "Le fichier CSV n'a pas pu être lu.",
                    exception
            );
        }
    }

    private static InvalidStockReceiptImportFileException invalid(
            StockReceiptImportFileErrorCode code,
            String message
    ) {
        return new InvalidStockReceiptImportFileException(code, message);
    }
}
