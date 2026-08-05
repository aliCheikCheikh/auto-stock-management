package com.aliCheikh.stock.infrastructure.persistence.entity;

import com.aliCheikh.stock.application.dto.StockReceiptImportIssueCode;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowAction;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "stock_receipt_import_row_result")
public class StockReceiptImportRowResultJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "import_id", nullable = false)
    private UUID importId;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private StockReceiptImportRowExecutionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private StockReceiptImportRowAction action;

    @Column(name = "product_reference", nullable = false, length = 100)
    private String productReference;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived;

    @Column(name = "issue_field", length = 50)
    private String issueField;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_code", length = 50)
    private StockReceiptImportIssueCode issueCode;

    @Column(name = "issue_message", length = 500)
    private String issueMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockReceiptImportRowResultJpaEntity() {
    }

    private StockReceiptImportRowResultJpaEntity(
            UUID id,
            UUID importId,
            int lineNumber,
            StockReceiptImportRowExecutionStatus status,
            StockReceiptImportRowAction action,
            String productReference,
            String productName,
            UUID productId,
            int quantityReceived,
            String issueField,
            StockReceiptImportIssueCode issueCode,
            String issueMessage,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.importId = Objects.requireNonNull(importId, "importId cannot be null");
        this.lineNumber = lineNumber;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.productReference = Objects.requireNonNull(productReference, "productReference cannot be null");
        this.productName = Objects.requireNonNull(productName, "productName cannot be null");
        this.productId = productId;
        this.quantityReceived = quantityReceived;
        this.issueField = issueField;
        this.issueCode = issueCode;
        this.issueMessage = issueMessage;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public static StockReceiptImportRowResultJpaEntity of(
            UUID id,
            UUID importId,
            int lineNumber,
            StockReceiptImportRowExecutionStatus status,
            StockReceiptImportRowAction action,
            String productReference,
            String productName,
            UUID productId,
            int quantityReceived,
            String issueField,
            StockReceiptImportIssueCode issueCode,
            String issueMessage,
            Instant createdAt
    ) {
        return new StockReceiptImportRowResultJpaEntity(
                id, importId, lineNumber, status, action, productReference, productName,
                productId, quantityReceived, issueField, issueCode, issueMessage, createdAt);
    }

    public UUID getImportId() { return importId; }
    public int getLineNumber() { return lineNumber; }
    public StockReceiptImportRowExecutionStatus getStatus() { return status; }
    public StockReceiptImportRowAction getAction() { return action; }
    public String getProductReference() { return productReference; }
    public String getProductName() { return productName; }
    public UUID getProductId() { return productId; }
    public int getQuantityReceived() { return quantityReceived; }
    public String getIssueField() { return issueField; }
    public StockReceiptImportIssueCode getIssueCode() { return issueCode; }
    public String getIssueMessage() { return issueMessage; }
}
