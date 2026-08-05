package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.StockReceiptImportCategoryCandidate;
import com.aliCheikh.stock.application.dto.StockReceiptImportFile;
import com.aliCheikh.stock.application.dto.StockReceiptImportProductCandidate;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowAction;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowData;
import com.aliCheikh.stock.application.dto.StockReceiptImportSummary;
import com.aliCheikh.stock.application.port.StockReceiptImportCategoryQueryPort;
import com.aliCheikh.stock.application.port.StockReceiptImportProductQueryPort;
import com.aliCheikh.stock.application.port.StockReceiptImportReader;
import com.aliCheikh.stock.application.service.StockReceiptImportRowPreparator;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PrepareStockReceiptImportUseCaseTest {

    private StockReceiptImportReader reader;
    private StockReceiptImportProductQueryPort productQueryPort;
    private StockReceiptImportCategoryQueryPort categoryQueryPort;
    private StorageLocationRepository storageLocationRepository;

    private PrepareStockReceiptImportUseCase useCase;
    private ShopId shopId;
    private LocationId shopFloorId;
    private LocationId backstockId;

    @BeforeEach
    void setUp() {
        reader = mock(StockReceiptImportReader.class);
        productQueryPort = mock(StockReceiptImportProductQueryPort.class);
        categoryQueryPort = mock(StockReceiptImportCategoryQueryPort.class);
        storageLocationRepository = mock(StorageLocationRepository.class);
        shopId = ShopId.generate();
        shopFloorId = LocationId.generate();
        backstockId = LocationId.generate();
        useCase = new PrepareStockReceiptImportUseCase(
                reader,
                productQueryPort,
                categoryQueryPort,
                storageLocationRepository,
                new StockReceiptImportRowPreparator()
        );

        given(storageLocationRepository.findByShopId(shopId)).willReturn(List.of(
                new StorageLocation(shopFloorId, shopId, LocationType.SHOP_FLOOR, "Surface", 0),
                new StorageLocation(backstockId, shopId, LocationType.BACKSTOCK, "Réserve", 0)
        ));
    }

    @Test
    void prepares_a_new_product_without_writing_stock() {
        StockReceiptImportFile file = file("produits.csv");
        CategoryId categoryId = CategoryId.generate();
        given(reader.read(file)).willReturn(List.of(row(
                2, " REF-001 ", "Filtre à huile", "Moteur", "12500", "5", "10", "20"
        )));
        given(productQueryPort.findCandidates(anySet(), anySet())).willReturn(List.of());
        given(categoryQueryPort.findByNames(anySet())).willReturn(List.of(
                new StockReceiptImportCategoryCandidate(categoryId, "Moteur")
        ));

        var preview = useCase.execute(file, shopId);

        assertThat(preview.rows()).hasSize(1);
        var prepared = preview.rows().get(0);
        assertThat(prepared.action()).isEqualTo(StockReceiptImportRowAction.CREATE_PRODUCT);
        assertThat(prepared.reference()).isEqualTo("REF-001");
        assertThat(prepared.name()).isEqualTo("Filtre à huile");
        assertThat(prepared.categoryName()).isEqualTo("Moteur");
        assertThat(prepared.categoryId()).isEqualTo(categoryId);
        assertThat(prepared.unitPrice().getAmount()).isEqualByComparingTo(new BigDecimal("12500"));
        assertThat(prepared.minimumGlobalThreshold()).isEqualTo(5);
        assertThat(prepared.distributions())
                .extracting(distribution -> distribution.locationId())
                .containsExactly(shopFloorId, backstockId);
        assertThat(prepared.distributions())
                .extracting(distribution -> distribution.quantity())
                .containsExactly(10, 20);
        assertThat(prepared.issues()).isEmpty();
        assertThat(preview.summary()).isEqualTo(new StockReceiptImportSummary(1, 1, 0, 0));

        verify(productQueryPort).findCandidates(Set.of("ref-001"), Set.of("filtre à huile"));
        verify(categoryQueryPort).findByNames(Set.of("moteur"));
    }

    @Test
    void receives_stock_for_an_existing_product_without_requiring_creation_fields() {
        StockReceiptImportFile file = file("stock.csv");
        ProductId productId = ProductId.generate();
        given(reader.read(file)).willReturn(List.of(row(
                2, "ref-existante", "", "", "", "", "3", "0"
        )));
        given(productQueryPort.findCandidates(anySet(), anySet())).willReturn(List.of(
                new StockReceiptImportProductCandidate(
                        productId, "REF-EXISTANTE", "Filtre existant", true
                )
        ));
        given(categoryQueryPort.findByNames(anySet())).willReturn(List.of());

        var preview = useCase.execute(file, shopId);

        var prepared = preview.rows().get(0);
        assertThat(prepared.action()).isEqualTo(StockReceiptImportRowAction.RECEIVE_EXISTING);
        assertThat(prepared.productId()).isEqualTo(productId);
        assertThat(prepared.reference()).isEqualTo("REF-EXISTANTE");
        assertThat(prepared.name()).isEqualTo("Filtre existant");
        assertThat(prepared.distributions()).singleElement().satisfies(distribution -> {
            assertThat(distribution.locationId()).isEqualTo(shopFloorId);
            assertThat(distribution.quantity()).isEqualTo(3);
        });
        assertThat(prepared.issues()).isEmpty();
        assertThat(preview.summary()).isEqualTo(new StockReceiptImportSummary(1, 0, 1, 0));
    }

    @Test
    void rejects_every_occurrence_of_a_duplicate_reference_in_the_file() {
        StockReceiptImportFile file = file("doublons.csv");
        given(reader.read(file)).willReturn(List.of(
                row(2, "REF-001", "Filtre A", "Moteur", "1000", "0", "1", "0"),
                row(3, " ref-001 ", "Filtre B", "Moteur", "2000", "0", "2", "0")
        ));
        given(productQueryPort.findCandidates(anySet(), anySet())).willReturn(List.of());
        given(categoryQueryPort.findByNames(anySet())).willReturn(List.of(
                new StockReceiptImportCategoryCandidate(CategoryId.generate(), "Moteur")
        ));

        var preview = useCase.execute(file, shopId);

        assertThat(preview.rows()).allSatisfy(row -> {
            assertThat(row.action()).isEqualTo(StockReceiptImportRowAction.REJECT);
            assertThat(row.issues())
                    .extracting(issue -> issue.code().name())
                    .contains("DUPLICATE_REFERENCE_IN_FILE");
        });
        assertThat(preview.summary()).isEqualTo(new StockReceiptImportSummary(2, 0, 0, 2));
    }

    @Test
    void explains_invalid_values_in_french_on_their_source_line() {
        StockReceiptImportFile file = file("erreurs.csv");
        given(reader.read(file)).willReturn(List.of(row(
                8, "REF-008", "Rotule", "Famille inconnue", "gratuit", "-1", "0", "0"
        )));
        given(productQueryPort.findCandidates(anySet(), anySet())).willReturn(List.of());
        given(categoryQueryPort.findByNames(anySet())).willReturn(List.of());

        var preview = useCase.execute(file, shopId);

        var rejected = preview.rows().get(0);
        assertThat(rejected.lineNumber()).isEqualTo(8);
        assertThat(rejected.action()).isEqualTo(StockReceiptImportRowAction.REJECT);
        assertThat(rejected.issues())
                .extracting(issue -> issue.code().name())
                .containsExactlyInAnyOrder(
                        "INVALID_UNIT_PRICE",
                        "NEGATIVE_THRESHOLD",
                        "MISSING_QUANTITY",
                        "UNKNOWN_CATEGORY"
                );
        assertThat(rejected.issues())
                .extracting(issue -> issue.message())
                .allSatisfy(message -> assertThat(message).isNotBlank());
    }

    private StockReceiptImportFile file(String filename) {
        return new StockReceiptImportFile(filename, "contenu".getBytes(StandardCharsets.UTF_8));
    }

    private StockReceiptImportRowData row(
            int lineNumber,
            String reference,
            String name,
            String category,
            String unitPrice,
            String threshold,
            String shopFloorQuantity,
            String backstockQuantity
    ) {
        return new StockReceiptImportRowData(
                lineNumber,
                reference,
                name,
                category,
                unitPrice,
                threshold,
                shopFloorQuantity,
                backstockQuantity
        );
    }
}
