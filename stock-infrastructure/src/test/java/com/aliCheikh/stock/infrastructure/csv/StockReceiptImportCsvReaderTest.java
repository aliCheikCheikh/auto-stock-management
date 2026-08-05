package com.aliCheikh.stock.infrastructure.csv;

import com.aliCheikh.stock.application.dto.StockReceiptImportFile;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowData;
import com.aliCheikh.stock.application.exception.InvalidStockReceiptImportFileException;
import com.aliCheikh.stock.application.exception.StockReceiptImportFileErrorCode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockReceiptImportCsvReaderTest {

    private static final String HEADERS = String.join(";",
            "reference",
            "nom_produit",
            "categorie",
            "prix_unitaire_xaf",
            "seuil_alerte",
            "quantite_surface",
            "quantite_reserve"
    );

    private final StockReceiptImportCsvReader reader = new StockReceiptImportCsvReader();

    @Test
    void shouldReadUtf8CsvWithBomQuotedCellsAndHeadersInAnyOrder() {
        String csv = "\uFEFFquantite_reserve;reference;categorie;nom_produit;seuil_alerte;prix_unitaire_xaf;quantite_surface\n"
                + "3;00042;Filtres;\"Filtre; huile\";2;12500;7\n";

        List<StockReceiptImportRowData> rows = reader.read(file("produits.csv", csv));

        assertThat(rows).containsExactly(new StockReceiptImportRowData(
                2,
                "00042",
                "Filtre; huile",
                "Filtres",
                "12500",
                "2",
                "7",
                "3"
        ));
    }

    @Test
    void shouldReadExcelCsvUsingCommaDelimiter() {
        String csv = HEADERS.replace(';', ',') + "\n"
                + "REF-007,\"Filtre, huile\",Filtres,\"12500,50\",5,10,20\n";

        List<StockReceiptImportRowData> rows = reader.read(file(" modele-import-produits.csv", csv));

        assertThat(rows).containsExactly(new StockReceiptImportRowData(
                2,
                "REF-007",
                "Filtre, huile",
                "Filtres",
                "12500,50",
                "5",
                "10",
                "20"
        ));
    }

    @Test
    void shouldPreserveQuotedLineBreaksAndIgnoreBlankRecords() {
        String csv = HEADERS + "\r\n"
                + "REF-1;\"Filtre\nhuile\";Filtres;12000;2;4;1\r\n"
                + ";;;;;;\r\n";

        List<StockReceiptImportRowData> rows = reader.read(file("produits.csv", csv));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.lineNumber()).isEqualTo(2);
            assertThat(row.name()).isEqualTo("Filtre\nhuile");
        });
    }

    @Test
    void shouldRejectNonCsvFilename() {
        assertError("produits.txt", HEADERS + "\n", StockReceiptImportFileErrorCode.UNSUPPORTED_FILE);
    }

    @Test
    void shouldRejectInvalidUtf8() {
        StockReceiptImportFile file = new StockReceiptImportFile("produits.csv", new byte[]{(byte) 0xC3, 0x28});

        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(InvalidStockReceiptImportFileException.class)
                .extracting("code")
                .isEqualTo(StockReceiptImportFileErrorCode.INVALID_ENCODING);
    }

    @Test
    void shouldRejectMissingUnknownAndDuplicateHeaders() {
        assertError("produits.csv", HEADERS.replace(";categorie", ""), StockReceiptImportFileErrorCode.INVALID_HEADER);
        assertError("produits.csv", HEADERS + ";commentaire", StockReceiptImportFileErrorCode.INVALID_HEADER);
        assertError("produits.csv", HEADERS.replace("categorie", "reference"), StockReceiptImportFileErrorCode.INVALID_HEADER);
    }

    @Test
    void shouldRejectUnsupportedDelimiter() {
        String csv = HEADERS.replace(';', '\t') + "\n"
                + "REF-1\tFiltre\tFiltres\t12000\t2\t4\t1\n";

        assertError("produits.csv", csv, StockReceiptImportFileErrorCode.INVALID_HEADER);
    }

    @Test
    void shouldRejectHeaderOnlyFile() {
        assertError("produits.csv", HEADERS + "\n", StockReceiptImportFileErrorCode.EMPTY_FILE);
    }

    @Test
    void shouldRejectInconsistentRows() {
        assertError(
                "produits.csv",
                HEADERS + "\nREF-1;Filtre;Filtres;12000;2;4\n",
                StockReceiptImportFileErrorCode.MALFORMED_CSV
        );
    }

    @Test
    void shouldRejectUnclosedQuotedCell() {
        assertError(
                "produits.csv",
                HEADERS + "\nREF-1;\"Filtre;Filtres;12000;2;4;1\n",
                StockReceiptImportFileErrorCode.MALFORMED_CSV
        );
    }

    @Test
    void shouldRejectMoreThanMaximumRows() {
        StringBuilder csv = new StringBuilder(HEADERS).append('\n');
        IntStream.rangeClosed(1, StockReceiptImportCsvReader.MAX_ROWS + 1)
                .forEach(index -> csv.append("REF-")
                        .append(index)
                        .append(";Produit;Categorie;1000;1;1;0\n"));

        assertError("produits.csv", csv.toString(), StockReceiptImportFileErrorCode.TOO_MANY_ROWS);
    }

    private static StockReceiptImportFile file(String filename, String csv) {
        return new StockReceiptImportFile(filename, csv.getBytes(StandardCharsets.UTF_8));
    }

    private void assertError(String filename, String csv, StockReceiptImportFileErrorCode expectedCode) {
        assertThatThrownBy(() -> reader.read(file(filename, csv)))
                .isInstanceOf(InvalidStockReceiptImportFileException.class)
                .extracting("code")
                .isEqualTo(expectedCode);
    }
}
