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

/** Transforme une ligne brute en ligne exécutable ou en rejet explicite. */
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
                    "Indiquez une quantité en surface de vente ou en réserve.");
        }

        List<TargetLocation> distributions = buildDistributions(
                shopFloorQuantity, backstockQuantity, locationIds, issues);

        StockReceiptImportProductCandidate existing = productsByReference.get(normalizedReference);
        if (existing != null) {
            if (!existing.active()) {
                addIssue(issues, "reference", StockReceiptImportIssueCode.INACTIVE_PRODUCT,
                        "Cette référence appartient à un produit désactivé.");
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
                    "La référence est obligatoire.");
        } else if (reference.length() > MAX_REFERENCE_LENGTH) {
            addIssue(issues, "reference", StockReceiptImportIssueCode.REFERENCE_TOO_LONG,
                    "La référence ne peut pas dépasser 100 caractères.");
        }
        if (!normalizedReference.isBlank() && referenceOccurrences.getOrDefault(normalizedReference, 0L) > 1) {
            addIssue(issues, "reference", StockReceiptImportIssueCode.DUPLICATE_REFERENCE_IN_FILE,
                    "Cette référence apparaît plusieurs fois dans le fichier.");
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
                    "Le nom du produit est obligatoire pour une nouvelle référence.");
        } else if (name.length() > MAX_NAME_LENGTH) {
            addIssue(issues, "nom_produit", StockReceiptImportIssueCode.NAME_TOO_LONG,
                    "Le nom du produit ne peut pas dépasser 255 caractères.");
        }
        if (!normalizedName.isBlank() && nameOccurrences.getOrDefault(normalizedName, 0L) > 1) {
            addIssue(issues, "nom_produit", StockReceiptImportIssueCode.DUPLICATE_NAME_IN_FILE,
                    "Ce nom de produit apparaît plusieurs fois dans le fichier.");
        }
        if (productsByName.containsKey(normalizedName)) {
            addIssue(issues, "nom_produit", StockReceiptImportIssueCode.PRODUCT_NAME_ALREADY_USED,
                    "Ce nom appartient déjà à un autre produit.");
        }

        String categoryName = row.categoryName().trim();
        if (categoryName.isBlank()) {
            addIssue(issues, "categorie", StockReceiptImportIssueCode.MISSING_CATEGORY,
                    "La famille de pièces est obligatoire pour un nouveau produit.");
            return null;
        }
        StockReceiptImportCategoryCandidate category = categoriesByName.get(normalizeKey(categoryName));
        if (category == null) {
            addIssue(issues, "categorie", StockReceiptImportIssueCode.UNKNOWN_CATEGORY,
                    "Cette famille de pièces n'existe pas dans le logiciel.");
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
                    "Le prix unitaire doit être un montant positif avec au maximum deux décimales.");
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
                        "Le seuil d'alerte ne peut pas être négatif.");
                return null;
            }
            return threshold;
        } catch (NumberFormatException exception) {
            addIssue(issues, "seuil_alerte", StockReceiptImportIssueCode.INVALID_THRESHOLD,
                    "Le seuil d'alerte doit être un nombre entier.");
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
                        "La quantité ne peut pas être négative.");
                return new ParsedInteger(0, false);
            }
            return new ParsedInteger(quantity, true);
        } catch (NumberFormatException exception) {
            addIssue(issues, field, StockReceiptImportIssueCode.INVALID_QUANTITY,
                    "La quantité doit être un nombre entier.");
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
                    "L'emplacement " + locationLabel(type) + " est indisponible.");
            return;
        }
        distributions.add(new TargetLocation(locationId, quantity.value()));
    }

    private String locationLabel(LocationType type) {
        return type == LocationType.SHOP_FLOOR ? "surface de vente" : "réserve";
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
