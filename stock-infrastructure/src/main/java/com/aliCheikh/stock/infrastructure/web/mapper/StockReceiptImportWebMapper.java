package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionReport;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionSummary;
import com.aliCheikh.stock.application.dto.StockReceiptImportIssue;
import com.aliCheikh.stock.application.dto.StockReceiptImportPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionResult;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportSummary;
import com.aliCheikh.stock.application.dto.TargetLocation;
import com.aliCheikh.stock.infrastructure.web.dto.MoneyResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportDistributionResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportExecutionReportResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportExecutionSummaryResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportIssueResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportPreviewResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportRowExecutionResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportRowPreviewResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportSummaryResponse;

public final class StockReceiptImportWebMapper {

    private StockReceiptImportWebMapper() {
    }

    public static StockReceiptImportPreviewResponse toResponse(StockReceiptImportPreview preview) {
        return new StockReceiptImportPreviewResponse(
                preview.rows().stream().map(StockReceiptImportWebMapper::toResponse).toList(),
                toResponse(preview.summary())
        );
    }

    public static StockReceiptImportExecutionReportResponse toResponse(
            StockReceiptImportExecutionReport report
    ) {
        return new StockReceiptImportExecutionReportResponse(
                report.importId(),
                report.rows().stream().map(StockReceiptImportWebMapper::toResponse).toList(),
                toResponse(report.summary())
        );
    }

    private static StockReceiptImportRowPreviewResponse toResponse(StockReceiptImportRowPreview row) {
        return new StockReceiptImportRowPreviewResponse(
                row.lineNumber(),
                row.action().name(),
                row.reference(),
                row.name(),
                row.categoryName(),
                row.categoryId() == null ? null : row.categoryId().getValue(),
                MoneyResponse.from(row.unitPrice()),
                row.minimumGlobalThreshold(),
                row.productId() == null ? null : row.productId().getValue(),
                row.distributions().stream().map(StockReceiptImportWebMapper::toResponse).toList(),
                row.issues().stream().map(StockReceiptImportWebMapper::toResponse).toList()
        );
    }

    private static StockReceiptImportRowExecutionResponse toResponse(
            StockReceiptImportRowExecutionResult row
    ) {
        return new StockReceiptImportRowExecutionResponse(
                row.lineNumber(),
                row.status().name(),
                row.action().name(),
                row.reference(),
                row.name(),
                row.productId() == null ? null : row.productId().getValue(),
                row.quantityReceived(),
                row.issues().stream().map(StockReceiptImportWebMapper::toResponse).toList()
        );
    }

    private static StockReceiptImportDistributionResponse toResponse(TargetLocation distribution) {
        return new StockReceiptImportDistributionResponse(
                distribution.locationId().getValue(),
                distribution.quantity()
        );
    }

    private static StockReceiptImportIssueResponse toResponse(StockReceiptImportIssue issue) {
        return new StockReceiptImportIssueResponse(
                issue.field(), issue.code().name(), issue.message()
        );
    }

    private static StockReceiptImportSummaryResponse toResponse(StockReceiptImportSummary summary) {
        return new StockReceiptImportSummaryResponse(
                summary.totalRows(),
                summary.productsToCreate(),
                summary.existingProductsToReceive(),
                summary.invalidRows()
        );
    }

    private static StockReceiptImportExecutionSummaryResponse toResponse(
            StockReceiptImportExecutionSummary summary
    ) {
        return new StockReceiptImportExecutionSummaryResponse(
                summary.totalRows(),
                summary.selectedRows(),
                summary.importedRows(),
                summary.productsCreated(),
                summary.existingProductsReceived(),
                summary.ignoredInvalidRows(),
                summary.ignoredByUserRows(),
                summary.failedRows(),
                summary.totalQuantityReceived()
        );
    }
}
