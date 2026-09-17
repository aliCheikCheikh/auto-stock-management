package com.aliCheikh.stock.application.service;

import com.aliCheikh.stock.application.dto.StockReceiptImportCategoryCandidate;
import com.aliCheikh.stock.application.dto.StockReceiptImportIssue;
import com.aliCheikh.stock.application.dto.StockReceiptImportIssueCode;
import com.aliCheikh.stock.application.dto.StockReceiptImportProductCandidate;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowAction;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowData;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowPreview;
import com.aliCheikh.stock.application.dto.TargetLocation;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.LocationType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static com.aliCheikh.stock.application.service.StockReceiptImportNormalizer.normalizeKey;

/** Prepares a CSV row for execution or reports its validation issues. */
public final class StockReceiptImportRowPreparator {

    private static final Currency XAF = Currency.getInstance("XAF");
    private static final int MAX_REFERENCE_LENGTH = 100;
    private static final int MAX_NAME_LENGTH = 255;

    public StockReceiptImportRowPreview prepare(
            StockReceiptImportRowData row,
            Map<String, StockReceiptImportProductCandidate> productsByReference,
            Map<String, StockReceiptImportProductCandidate> productsByName,
            Map<String, StockReceiptImportCategoryCandidate> categoriesByName,
            Map<String, Long> referenceOccurrences,
            Map<String, Long> nameOccurrences,
            Map<LocationType, LocationId> locationIds
    ) {
        List<StockReceiptImportIssue> issues = new ArrayList<>();
        String reference = row.reference().trim();
        String name = row.name().trim();
        String normalizedReference = normalizeKey(reference);

        validateReference(reference, normalizedReference, referenceOccurrences, issues);
        ParsedInteger shopFloorQuantity = parseNonNegativeInteger(
                row.shopFloorQuantity(), "quantite_surface", issues);
        ParsedInteger backstockQuantity = parseNonNegativeInteger(
                row.backstockQuantity(), "quantite_reserve", issues);
        if (shopFloorQuantity.valid() && backstockQuantity.valid()
                && (long) shopFloorQuantity.value() + backstockQuantity.value() <= 0) {
            addIssue(issues, "quantites", StockReceiptImportIssueCode.MISSING_QUANTITY,
                    "Enter a quantity for the shop floor or backstock.");
        }

        List<TargetLocation> distributions = buildDistributions(
                shopFloorQuantity, backstockQuantity, locationIds, issues);

        StockReceiptImportProductCandidate existing = productsByReference.get(normalizedReference);
        if (existing != null) {
            if (!existing.active()) {
                addIssue(issues, "reference", StockReceiptImportIssueCode.INACTIVE_PRODUCT,
                        "This reference belongs to an inactive product.");
            }
            return preview(
                    row.lineNumber(), issues, existing.reference(), existing.name(),
                    row.categoryName().trim(), null,
                    null, null, existing.productId(), distributions,
                    StockReceiptImportRowAction.RECEIVE_EXISTING);
        }

        CategoryId categoryId = validateCreationFields(
                row, name, issues, productsByName, categoriesByName, nameOccurrences);
        Money unitPrice = parseUnitPrice(row.unitPriceAmount(), issues);
        Integer threshold = parseThreshold(row.minimumGlobalThreshold(), issues);

        return preview(
                row.lineNumber(), issues, reference, name, row.categoryName().trim(),
                categoryId, unitPrice, threshold,
                null, distributions, StockReceiptImportRowAction.CREATE_PRODUCT);
    }

    private void validateReference(
            String reference,
            String normalizedReference,
            Map<String, Long> referenceOccurrences,
            List<StockReceiptImportIssue> issues
    ) {
        if (reference.isBlank()) {
            addIssue(issues, "reference", StockReceiptImportIssueCode.MISSING_REFERENCE,
                    "Reference is required.");
        } else if (reference.length() > MAX_REFERENCE_LENGTH) {
            addIssue(issues, "reference", StockReceiptImportIssueCode.REFERENCE_TOO_LONG,
                    "Reference must not exceed 100 characters.");
        }
        if (!normalizedReference.isBlank() && referenceOccurrences.getOrDefault(normalizedReference, 0L) > 1) {
            addIssue(issues, "reference", StockReceiptImportIssueCode.DUPLICATE_REFERENCE_IN_FILE,
                    "This reference appears more than once in the file.");
        }
    }

    private CategoryId validateCreationFields(
            StockReceiptImportRowData row,
            String name,
            List<StockReceiptImportIssue> issues,
            Map<String, StockReceiptImportProductCandidate> productsByName,
            Map<String, StockReceiptImportCategoryCandidate> categoriesByName,
            Map<String, Long> nameOccurrences
    ) {
        String normalizedName = normalizeKey(name);
        if (name.isBlank()) {
            addIssue(issues, "nom_produit", StockReceiptImportIssueCode.MISSING_NAME,
                    "Product name is required for a new reference.");
        } else if (name.length() > MAX_NAME_LENGTH) {
            addIssue(issues, "nom_produit", StockReceiptImportIssueCode.NAME_TOO_LONG,
                    "Product name must not exceed 255 characters.");
        }
        if (!normalizedName.isBlank() && nameOccurrences.getOrDefault(normalizedName, 0L) > 1) {
            addIssue(issues, "nom_produit", StockReceiptImportIssueCode.DUPLICATE_NAME_IN_FILE,
                    "This product name appears more than once in the file.");
        }
        if (productsByName.containsKey(normalizedName)) {
            addIssue(issues, "nom_produit", StockReceiptImportIssueCode.PRODUCT_NAME_ALREADY_USED,
                    "This name already belongs to another product.");
        }

        String categoryName = row.categoryName().trim();
        if (categoryName.isBlank()) {
            addIssue(issues, "categorie", StockReceiptImportIssueCode.MISSING_CATEGORY,
                    "Category is required for a new product.");
            return null;
        }
        StockReceiptImportCategoryCandidate category = categoriesByName.get(normalizeKey(categoryName));
        if (category == null) {
            addIssue(issues, "categorie", StockReceiptImportIssueCode.UNKNOWN_CATEGORY,
                    "This category does not exist.");
            return null;
        }
        return category.categoryId();
    }

    private Money parseUnitPrice(String rawValue, List<StockReceiptImportIssue> issues) {
        try {
            String normalized = rawValue.trim().replace(" ", "").replace(',', '.');
            BigDecimal amount = new BigDecimal(normalized);
            if (amount.signum() <= 0 || amount.precision() > 15 || Math.max(amount.scale(), 0) > 2) {
                throw new NumberFormatException();
            }
            return Money.create(amount, XAF);
        } catch (NumberFormatException exception) {
            addIssue(issues, "prix_unitaire_xaf", StockReceiptImportIssueCode.INVALID_UNIT_PRICE,
                    "Unit price must be positive with at most two decimal places.");
            return null;
        }
    }

    private Integer parseThreshold(String rawValue, List<StockReceiptImportIssue> issues) {
        String normalized = rawValue.trim();
        if (normalized.isEmpty()) {
            return 0;
        }
        try {
            int threshold = Integer.parseInt(normalized);
            if (threshold < 0) {
                addIssue(issues, "seuil_alerte", StockReceiptImportIssueCode.NEGATIVE_THRESHOLD,
                        "Alert threshold must not be negative.");
                return null;
            }
            return threshold;
        } catch (NumberFormatException exception) {
            addIssue(issues, "seuil_alerte", StockReceiptImportIssueCode.INVALID_THRESHOLD,
                    "Alert threshold must be an integer.");
            return null;
        }
    }

    private ParsedInteger parseNonNegativeInteger(
            String rawValue,
            String field,
            List<StockReceiptImportIssue> issues
    ) {
        String normalized = rawValue.trim();
        if (normalized.isEmpty()) {
            return new ParsedInteger(0, true);
        }
        try {
            int quantity = Integer.parseInt(normalized);
            if (quantity < 0) {
                addIssue(issues, field, StockReceiptImportIssueCode.NEGATIVE_QUANTITY,
                        "Quantity must not be negative.");
                return new ParsedInteger(0, false);
            }
            return new ParsedInteger(quantity, true);
        } catch (NumberFormatException exception) {
            addIssue(issues, field, StockReceiptImportIssueCode.INVALID_QUANTITY,
                    "Quantity must be an integer.");
            return new ParsedInteger(0, false);
        }
    }

    private List<TargetLocation> buildDistributions(
            ParsedInteger shopFloorQuantity,
            ParsedInteger backstockQuantity,
            Map<LocationType, LocationId> locationIds,
            List<StockReceiptImportIssue> issues
    ) {
        List<TargetLocation> distributions = new ArrayList<>(2);
        addDistribution(distributions, LocationType.SHOP_FLOOR, shopFloorQuantity, locationIds, issues);
        addDistribution(distributions, LocationType.BACKSTOCK, backstockQuantity, locationIds, issues);
        return List.copyOf(distributions);
    }

    private void addDistribution(
            List<TargetLocation> distributions,
            LocationType type,
            ParsedInteger quantity,
            Map<LocationType, LocationId> locationIds,
            List<StockReceiptImportIssue> issues
    ) {
        if (!quantity.valid() || quantity.value() <= 0) {
            return;
        }
        LocationId locationId = locationIds.get(type);
        if (locationId == null) {
            addIssue(issues, "emplacement", StockReceiptImportIssueCode.MISSING_LOCATION,
                    "The " + locationLabel(type) + " location is unavailable.");
            return;
        }
        distributions.add(new TargetLocation(locationId, quantity.value()));
    }

    private String locationLabel(LocationType type) {
        return type == LocationType.SHOP_FLOOR ? "shop floor" : "backstock";
    }

    private StockReceiptImportRowPreview preview(
            int lineNumber,
            List<StockReceiptImportIssue> issues,
            String reference,
            String name,
            String categoryName,
            CategoryId categoryId,
            Money unitPrice,
            Integer threshold,
            ProductId productId,
            List<TargetLocation> distributions,
            StockReceiptImportRowAction readyAction
    ) {
        StockReceiptImportRowAction action = issues.isEmpty()
                ? readyAction
                : StockReceiptImportRowAction.REJECT;
        return new StockReceiptImportRowPreview(
                lineNumber, action, reference, name, categoryName, categoryId, unitPrice, threshold,
                productId, distributions, issues);
    }

    private void addIssue(
            List<StockReceiptImportIssue> issues,
            String field,
            StockReceiptImportIssueCode code,
            String message
    ) {
        issues.add(new StockReceiptImportIssue(field, code, message));
    }

    private record ParsedInteger(int value, boolean valid) {
    }
}
