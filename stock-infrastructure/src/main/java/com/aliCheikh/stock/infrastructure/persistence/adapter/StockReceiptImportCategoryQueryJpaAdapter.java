package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.StockReceiptImportCategoryCandidate;
import com.aliCheikh.stock.application.port.StockReceiptImportCategoryQueryPort;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.infrastructure.persistence.repository.CategoryJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public class StockReceiptImportCategoryQueryJpaAdapter implements StockReceiptImportCategoryQueryPort {

    private final CategoryJpaRepository categoryJpaRepository;

    public StockReceiptImportCategoryQueryJpaAdapter(CategoryJpaRepository categoryJpaRepository) {
        this.categoryJpaRepository = Objects.requireNonNull(
                categoryJpaRepository, "categoryJpaRepository cannot be null");
    }

    @Override
    public List<StockReceiptImportCategoryCandidate> findByNames(Set<String> normalizedNames) {
        Objects.requireNonNull(normalizedNames, "normalizedNames cannot be null");
        if (normalizedNames.isEmpty()) {
            return List.of();
        }
        return categoryJpaRepository.findByNormalizedNames(normalizedNames).stream()
                .map(entity -> new StockReceiptImportCategoryCandidate(
                        CategoryId.of(entity.getId()), entity.getName()))
                .toList();
    }
}
