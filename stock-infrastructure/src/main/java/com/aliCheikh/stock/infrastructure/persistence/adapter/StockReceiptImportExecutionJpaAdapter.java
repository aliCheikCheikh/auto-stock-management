package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionDescriptor;
import com.aliCheikh.stock.application.dto.StockReceiptImportIssue;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionResult;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionStatus;
import com.aliCheikh.stock.application.exception.StockReceiptImportExecutionConflictException;
import com.aliCheikh.stock.application.port.StockReceiptImportExecutionLedger;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockReceiptImportExecutionJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.StockReceiptImportRowResultJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.repository.StockReceiptImportExecutionJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.StockReceiptImportRowResultJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class StockReceiptImportExecutionJpaAdapter implements StockReceiptImportExecutionLedger {

    private final StockReceiptImportExecutionJpaRepository executionRepository;
    private final StockReceiptImportRowResultJpaRepository rowResultRepository;

    public StockReceiptImportExecutionJpaAdapter(
            StockReceiptImportExecutionJpaRepository executionRepository,
            StockReceiptImportRowResultJpaRepository rowResultRepository
    ) {
        this.executionRepository = Objects.requireNonNull(
                executionRepository, "executionRepository cannot be null");
        this.rowResultRepository = Objects.requireNonNull(
                rowResultRepository, "rowResultRepository cannot be null");
    }

    @Override
    @Transactional
    public void ensureExecution(StockReceiptImportExecutionDescriptor descriptor) {
        executionRepository.insertIfAbsent(
                descriptor.importId(),
                descriptor.fingerprint(),
                descriptor.shopId().getValue(),
                descriptor.userId().getValue(),
                Instant.now()
        );
        StockReceiptImportExecutionJpaEntity execution = executionRepository.findById(descriptor.importId())
                .orElseThrow(() -> new IllegalStateException("Import execution was not persisted"));
        if (!execution.matches(
                descriptor.fingerprint(),
                descriptor.shopId().getValue(),
                descriptor.userId().getValue()
        )) {
            throw new StockReceiptImportExecutionConflictException(descriptor.importId());
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockExecution(UUID importId) {
        executionRepository.findByIdForUpdate(importId)
                .orElseThrow(() -> new IllegalStateException("Unknown stock receipt import: " + importId));
    }

    @Override
    public Optional<StockReceiptImportRowExecutionResult> findRowResult(UUID importId, int lineNumber) {
        return rowResultRepository.findByImportIdAndLineNumber(importId, lineNumber)
                .map(this::toResult);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveRowResult(UUID importId, StockReceiptImportRowExecutionResult result) {
        if (result.status() != StockReceiptImportRowExecutionStatus.IMPORTED
                && result.status() != StockReceiptImportRowExecutionStatus.FAILED) {
            throw new IllegalArgumentException("Only attempted import rows belong in the durable ledger");
        }
        if (result.status() == StockReceiptImportRowExecutionStatus.FAILED
                && result.issues().size() != 1) {
            throw new IllegalArgumentException("A persisted import failure must contain exactly one issue");
        }
        StockReceiptImportIssue issue = result.issues().stream().findFirst().orElse(null);
        rowResultRepository.save(StockReceiptImportRowResultJpaEntity.of(
                UUID.randomUUID(),
                importId,
                result.lineNumber(),
                result.status(),
                result.action(),
                result.reference(),
                result.name(),
                result.productId() == null ? null : result.productId().getValue(),
                result.quantityReceived(),
                issue == null ? null : issue.field(),
                issue == null ? null : issue.code(),
                issue == null ? null : issue.message(),
                Instant.now()
        ));
    }

    private StockReceiptImportRowExecutionResult toResult(StockReceiptImportRowResultJpaEntity entity) {
        List<StockReceiptImportIssue> issues = entity.getIssueCode() == null
                ? List.of()
                : List.of(new StockReceiptImportIssue(
                        entity.getIssueField(), entity.getIssueCode(), entity.getIssueMessage()));
        return new StockReceiptImportRowExecutionResult(
                entity.getLineNumber(),
                entity.getStatus(),
                entity.getAction(),
                entity.getProductReference(),
                entity.getProductName(),
                entity.getProductId() == null ? null : ProductId.of(entity.getProductId()),
                entity.getQuantityReceived(),
                issues
        );
    }
}
