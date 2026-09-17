package com.aliCheikh.stock.infrastructure.csv;

import com.aliCheikh.stock.application.dto.StockReceiptImportFile;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowData;
import com.aliCheikh.stock.application.exception.InvalidStockReceiptImportFileException;
import com.aliCheikh.stock.application.exception.StockReceiptImportFileErrorCode;
import com.aliCheikh.stock.application.port.StockReceiptImportReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class StockReceiptImportCsvReader implements StockReceiptImportReader {

    public static final int MAX_ROWS = 500;

    private static final String REFERENCE = "reference";
    private static final String PRODUCT_NAME = "nom_produit";
    private static final String CATEGORY = "categorie";
    private static final String UNIT_PRICE = "prix_unitaire_xaf";
    private static final String MINIMUM_THRESHOLD = "seuil_alerte";
    private static final String SHOP_FLOOR_QUANTITY = "quantite_surface";
    private static final String BACKSTOCK_QUANTITY = "quantite_reserve";

    private static final List<String> EXPECTED_HEADERS = List.of(
            REFERENCE,
            PRODUCT_NAME,
            CATEGORY,
            UNIT_PRICE,
            MINIMUM_THRESHOLD,
            SHOP_FLOOR_QUANTITY,
            BACKSTOCK_QUANTITY
    );

    private static final List<Character> SUPPORTED_DELIMITERS = List.of(';', ',');

    @Override
    public List<StockReceiptImportRowData> read(StockReceiptImportFile file) {
        validateFilename(file.filename());
        String csv = decodeUtf8(file.content());
        if (csv.startsWith("\uFEFF")) {
            csv = csv.substring(1);
        }
        if (csv.isBlank()) {
            throw invalid(StockReceiptImportFileErrorCode.EMPTY_FILE,
                    "The CSV file is empty.");
        }

        CSVFormat format = format(detectDelimiter(csv));
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            validateHeaders(parser.getHeaderNames());
            return readRows(parser);
        } catch (InvalidStockReceiptImportFileException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw invalid(StockReceiptImportFileErrorCode.INVALID_HEADER,
                    "CSV headers are invalid or duplicated.", exception);
        } catch (IOException | UncheckedIOException exception) {
            throw invalid(StockReceiptImportFileErrorCode.MALFORMED_CSV,
                    "The CSV file is malformed. Check delimiters and quotation marks.", exception);
        }
    }

    private static char detectDelimiter(String csv) {
        List<Character> matchingDelimiters = new ArrayList<>();

        for (char delimiter : SUPPORTED_DELIMITERS) {
            try (CSVParser parser = format(delimiter).parse(new StringReader(csv))) {
                if (headersMatch(parser.getHeaderNames())) {
                    matchingDelimiters.add(delimiter);
                }
            } catch (IllegalArgumentException | IOException | UncheckedIOException ignored) {
                // This candidate does not describe a structurally valid header.
            }
        }

        if (matchingDelimiters.size() != 1) {
            throw invalid(StockReceiptImportFileErrorCode.INVALID_HEADER,
                    "Expected columns: " + String.join(", ", EXPECTED_HEADERS)
                            + ". Use a semicolon or comma delimiter.");
        }
        return matchingDelimiters.get(0);
    }

    private static CSVFormat format(char delimiter) {
        return CSVFormat.RFC4180.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
                .setAllowMissingColumnNames(false)
                .setIgnoreEmptyLines(false)
                .get();
    }

    private static void validateFilename(String filename) {
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw invalid(StockReceiptImportFileErrorCode.UNSUPPORTED_FILE,
                    "The file must use CSV format (.csv).");
        }
    }

    private static String decodeUtf8(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw invalid(StockReceiptImportFileErrorCode.INVALID_ENCODING,
                    "The file must be encoded in UTF-8.", exception);
        }
    }

    private static void validateHeaders(List<String> actualHeaders) {
        if (!headersMatch(actualHeaders)) {
            throw invalid(StockReceiptImportFileErrorCode.INVALID_HEADER,
                    "Expected columns: " + String.join(", ", EXPECTED_HEADERS) + ".");
        }
    }

    private static boolean headersMatch(List<String> actualHeaders) {
        Set<String> actualHeaderSet = new HashSet<>(actualHeaders);
        Set<String> expectedHeaderSet = Set.copyOf(EXPECTED_HEADERS);
        return actualHeaders.size() == EXPECTED_HEADERS.size() && actualHeaderSet.equals(expectedHeaderSet);
    }

    private static List<StockReceiptImportRowData> readRows(CSVParser parser) {
        List<StockReceiptImportRowData> rows = new ArrayList<>();
        int sourceRow = 2;

        for (CSVRecord record : parser) {
            if (isBlank(record)) {
                sourceRow++;
                continue;
            }
            if (!record.isConsistent()) {
                throw invalid(StockReceiptImportFileErrorCode.MALFORMED_CSV,
                        "Row " + sourceRow + " does not contain the expected number of columns.");
            }
            if (rows.size() == MAX_ROWS) {
                throw invalid(StockReceiptImportFileErrorCode.TOO_MANY_ROWS,
                        "The file must not contain more than " + MAX_ROWS + " product rows.");
            }

            rows.add(toRowData(record, sourceRow));
            sourceRow++;
        }

        if (rows.isEmpty()) {
            throw invalid(StockReceiptImportFileErrorCode.EMPTY_FILE,
                    "The file contains no product rows.");
        }
        return List.copyOf(rows);
    }

    private static boolean isBlank(CSVRecord record) {
        for (String value : record) {
            if (!value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static StockReceiptImportRowData toRowData(CSVRecord record, int sourceRow) {
        return new StockReceiptImportRowData(
                sourceRow,
                record.get(REFERENCE),
                record.get(PRODUCT_NAME),
                record.get(CATEGORY),
                record.get(UNIT_PRICE),
                record.get(MINIMUM_THRESHOLD),
                record.get(SHOP_FLOOR_QUANTITY),
                record.get(BACKSTOCK_QUANTITY)
        );
    }

    private static InvalidStockReceiptImportFileException invalid(
            StockReceiptImportFileErrorCode code,
            String message
    ) {
        return new InvalidStockReceiptImportFileException(code, message);
    }

    private static InvalidStockReceiptImportFileException invalid(
            StockReceiptImportFileErrorCode code,
            String message,
            Throwable cause
    ) {
        return new InvalidStockReceiptImportFileException(code, message, cause);
    }
}
