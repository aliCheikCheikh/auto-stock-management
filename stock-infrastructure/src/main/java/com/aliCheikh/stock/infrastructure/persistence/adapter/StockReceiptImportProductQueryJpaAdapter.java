package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.StockReceiptImportProductCandidate;
import com.aliCheikh.stock.application.port.StockReceiptImportProductQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Repository
public class StockReceiptImportProductQueryJpaAdapter implements StockReceiptImportProductQueryPort {

    private final ProductJpaRepository productJpaRepository;

    public StockReceiptImportProductQueryJpaAdapter(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = Objects.requireNonNull(
                productJpaRepository, "productJpaRepository cannot be null");
    }

    @Override
    public List<StockReceiptImportProductCandidate> findCandidates(
            Set<String> normalizedReferences,
            Set<String> normalizedNames
    ) {
        Objects.requireNonNull(normalizedReferences, "normalizedReferences cannot be null");
        Objects.requireNonNull(normalizedNames, "normalizedNames cannot be null");

        Map<ProductId, StockReceiptImportProductCandidate> candidates = new LinkedHashMap<>();
        if (!normalizedReferences.isEmpty()) {
            productJpaRepository.findByNormalizedReferences(normalizedReferences)
                    .forEach(entity -> addCandidate(candidates, entity));
        }
        if (!normalizedNames.isEmpty()) {
            productJpaRepository.findByNormalizedNames(normalizedNames)
                    .forEach(entity -> addCandidate(candidates, entity));
        }
        return List.copyOf(candidates.values());
    }

    private void addCandidate(
            Map<ProductId, StockReceiptImportProductCandidate> candidates,
            ProductJpaEntity entity
    ) {
        ProductId productId = ProductId.of(entity.getId());
        candidates.putIfAbsent(productId, new StockReceiptImportProductCandidate(
                productId,
                entity.getReference(),
                entity.getName(),
                entity.isActive()
        ));
    }
}
