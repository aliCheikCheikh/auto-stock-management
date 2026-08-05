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

    private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW)
            .setAllowMissingColumnNames(false)
            .setIgnoreEmptyLines(false)
            .get();

    @Override
    public List<StockReceiptImportRowData> read(StockReceiptImportFile file) {
        validateFilename(file.filename());
        String csv = decodeUtf8(file.content());
        if (csv.startsWith("\uFEFF")) {
            csv = csv.substring(1);
        }
        if (csv.isBlank()) {
            throw invalid(StockReceiptImportFileErrorCode.EMPTY_FILE,
                    "Le fichier CSV est vide.");
        }

        try (CSVParser parser = FORMAT.parse(new StringReader(csv))) {
            validateHeaders(parser.getHeaderNames());
            return readRows(parser);
        } catch (InvalidStockReceiptImportFileException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw invalid(StockReceiptImportFileErrorCode.INVALID_HEADER,
                    "Les en-têtes du fichier CSV sont invalides ou dupliqués.", exception);
        } catch (IOException | UncheckedIOException exception) {
            throw invalid(StockReceiptImportFileErrorCode.MALFORMED_CSV,
                    "Le fichier CSV est mal formé. Vérifiez les séparateurs et les guillemets.", exception);
        }
    }

    private static void validateFilename(String filename) {
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw invalid(StockReceiptImportFileErrorCode.UNSUPPORTED_FILE,
                    "Le fichier doit être au format CSV (.csv).");
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
                    "Le fichier doit être enregistré en UTF-8.", exception);
        }
    }

    private static void validateHeaders(List<String> actualHeaders) {
        Set<String> actualHeaderSet = new HashSet<>(actualHeaders);
        Set<String> expectedHeaderSet = Set.copyOf(EXPECTED_HEADERS);
        if (actualHeaders.size() != EXPECTED_HEADERS.size() || !actualHeaderSet.equals(expectedHeaderSet)) {
            throw invalid(StockReceiptImportFileErrorCode.INVALID_HEADER,
                    "Les colonnes attendues sont : " + String.join(", ", EXPECTED_HEADERS) + ".");
        }
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
                        "La ligne " + sourceRow + " ne contient pas le nombre de colonnes attendu.");
            }
            if (rows.size() == MAX_ROWS) {
                throw invalid(StockReceiptImportFileErrorCode.TOO_MANY_ROWS,
                        "Le fichier ne peut pas contenir plus de " + MAX_ROWS + " lignes de produits.");
            }

            rows.add(toRowData(record, sourceRow));
            sourceRow++;
        }

        if (rows.isEmpty()) {
            throw invalid(StockReceiptImportFileErrorCode.EMPTY_FILE,
                    "Le fichier ne contient aucune ligne de produit.");
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
