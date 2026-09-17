package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.StockReceiptImportCategoryCandidate;
import com.aliCheikh.stock.application.dto.StockReceiptImportFile;
import com.aliCheikh.stock.application.dto.StockReceiptImportPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportProductCandidate;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowAction;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowData;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportSummary;
import com.aliCheikh.stock.application.port.StockReceiptImportCategoryQueryPort;
import com.aliCheikh.stock.application.port.StockReceiptImportProductQueryPort;
import com.aliCheikh.stock.application.port.StockReceiptImportReader;
import com.aliCheikh.stock.application.service.StockReceiptImportNormalizer;
import com.aliCheikh.stock.application.service.StockReceiptImportRowPreparator;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.aliCheikh.stock.application.service.StockReceiptImportNormalizer.normalizeKey;

/** Previews an import without creating products or changing stock. */
public class PrepareStockReceiptImportUseCase {

    private final StockReceiptImportReader reader;
    private final StockReceiptImportProductQueryPort productQueryPort;
    private final StockReceiptImportCategoryQueryPort categoryQueryPort;
    private final StorageLocationRepository storageLocationRepository;
    private final StockReceiptImportRowPreparator rowPreparator;

    public PrepareStockReceiptImportUseCase(
            StockReceiptImportReader reader,
            StockReceiptImportProductQueryPort productQueryPort,
            StockReceiptImportCategoryQueryPort categoryQueryPort,
            StorageLocationRepository storageLocationRepository,
            StockReceiptImportRowPreparator rowPreparator
    ) {
        this.reader = Objects.requireNonNull(reader, "reader cannot be null");
        this.productQueryPort = Objects.requireNonNull(productQueryPort, "productQueryPort cannot be null");
        this.categoryQueryPort = Objects.requireNonNull(categoryQueryPort, "categoryQueryPort cannot be null");
        this.storageLocationRepository = Objects.requireNonNull(
                storageLocationRepository, "storageLocationRepository cannot be null");
        this.rowPreparator = Objects.requireNonNull(rowPreparator, "rowPreparator cannot be null");
    }

    public StockReceiptImportPreview execute(StockReceiptImportFile file, ShopId shopId) {
        Objects.requireNonNull(file, "file cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");

        List<StockReceiptImportRowData> rows = List.copyOf(reader.read(file));
        Set<String> references = normalizedNonBlankValues(rows, StockReceiptImportRowData::reference);
        Set<String> names = normalizedNonBlankValues(rows, StockReceiptImportRowData::name);
        Set<String> categories = normalizedNonBlankValues(rows, StockReceiptImportRowData::categoryName);

        List<StockReceiptImportProductCandidate> productCandidates =
                productQueryPort.findCandidates(references, names);
        Map<String, StockReceiptImportProductCandidate> productsByReference = indexByNormalized(
                productCandidates, StockReceiptImportProductCandidate::reference);
        Map<String, StockReceiptImportProductCandidate> productsByName = indexByNormalized(
                productCandidates, StockReceiptImportProductCandidate::name);
        Map<String, StockReceiptImportCategoryCandidate> categoriesByName = indexByNormalized(
                categoryQueryPort.findByNames(categories), StockReceiptImportCategoryCandidate::name);
        Map<String, Long> referenceOccurrences = occurrences(rows, StockReceiptImportRowData::reference);
        Map<String, Long> nameOccurrences = occurrences(rows, StockReceiptImportRowData::name);
        Map<LocationType, LocationId> locationIds = storageLocationRepository.findByShopId(shopId).stream()
                .collect(Collectors.toMap(
                        StorageLocation::getLocationType,
                        StorageLocation::getLocationId,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        List<StockReceiptImportRowPreview> preparedRows = rows.stream()
                .map(row -> rowPreparator.prepare(
                        row,
                        productsByReference,
                        productsByName,
                        categoriesByName,
                        referenceOccurrences,
                        nameOccurrences,
                        locationIds
                ))
                .toList();

        return new StockReceiptImportPreview(preparedRows, summarize(preparedRows));
    }

    private StockReceiptImportSummary summarize(List<StockReceiptImportRowPreview> rows) {
        int creations = count(rows, StockReceiptImportRowAction.CREATE_PRODUCT);
        int receptions = count(rows, StockReceiptImportRowAction.RECEIVE_EXISTING);
        int invalid = count(rows, StockReceiptImportRowAction.REJECT);
        return new StockReceiptImportSummary(rows.size(), creations, receptions, invalid);
    }

    private int count(List<StockReceiptImportRowPreview> rows, StockReceiptImportRowAction action) {
        return (int) rows.stream().filter(row -> row.action() == action).count();
    }

    private <T> Map<String, T> indexByNormalized(List<T> values, Function<T, String> keyExtractor) {
        Map<String, T> indexed = new HashMap<>();
        for (T value : values) {
            indexed.putIfAbsent(normalizeKey(keyExtractor.apply(value)), value);
        }
        return indexed;
    }

    private Set<String> normalizedNonBlankValues(
            List<StockReceiptImportRowData> rows,
            Function<StockReceiptImportRowData, String> extractor
    ) {
        return rows.stream()
                .map(extractor)
                .map(StockReceiptImportNormalizer::normalizeKey)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private Map<String, Long> occurrences(
            List<StockReceiptImportRowData> rows,
            Function<StockReceiptImportRowData, String> extractor
    ) {
        return rows.stream()
                .map(extractor)
                .map(StockReceiptImportNormalizer::normalizeKey)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }
}
