package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionCommand;
import com.aliCheikh.stock.application.dto.StockReceiptImportFile;
import com.aliCheikh.stock.application.usecase.ExecuteStockReceiptImportUseCase;
import com.aliCheikh.stock.application.usecase.PrepareStockReceiptImportUseCase;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportExecutionReportResponse;
import com.aliCheikh.stock.infrastructure.web.dto.StockReceiptImportPreviewResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.StockReceiptImportUploadMapper;
import com.aliCheikh.stock.infrastructure.web.mapper.StockReceiptImportWebMapper;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/stock-receipts")
public class StockReceiptImportController {

    private static final MediaType CSV_UTF_8 = MediaType.parseMediaType("text/csv;charset=UTF-8");
    private static final byte[] CSV_TEMPLATE = (
            "\uFEFFreference;nom_produit;categorie;prix_unitaire_xaf;seuil_alerte;"
                    + "quantite_surface;quantite_reserve\r\n"
    ).getBytes(StandardCharsets.UTF_8);

    private final PrepareStockReceiptImportUseCase prepareUseCase;
    private final ExecuteStockReceiptImportUseCase executeUseCase;

    public StockReceiptImportController(
            PrepareStockReceiptImportUseCase prepareUseCase,
            ExecuteStockReceiptImportUseCase executeUseCase
    ) {
        this.prepareUseCase = prepareUseCase;
        this.executeUseCase = executeUseCase;
    }

    @GetMapping(value = "/import-template", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> downloadTemplate() {
        return ResponseEntity.ok()
                .contentType(CSV_UTF_8)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modele-import-produits.csv\"")
                .body(CSV_TEMPLATE.clone());
    }

    @PostMapping(value = "/import-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StockReceiptImportPreviewResponse preview(
            @RequestPart("file") MultipartFile upload,
            @RequestParam UUID shopId
    ) {
        StockReceiptImportFile file = StockReceiptImportUploadMapper.toFile(upload);
        return StockReceiptImportWebMapper.toResponse(
                prepareUseCase.execute(file, ShopId.of(shopId))
        );
    }

    @PostMapping(value = "/import-executions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StockReceiptImportExecutionReportResponse execute(
            @RequestPart("file") MultipartFile upload,
            @RequestParam UUID shopId,
            @RequestParam UUID importId,
            @RequestParam @Size(min = 1, max = 500) Set<@Min(2) Integer> selectedLineNumbers,
            Authentication authentication
    ) {
        UUID userId = (UUID) authentication.getPrincipal();
        StockReceiptImportExecutionCommand command = new StockReceiptImportExecutionCommand(
                importId,
                StockReceiptImportUploadMapper.toFile(upload),
                ShopId.of(shopId),
                UserId.of(userId),
                selectedLineNumbers
        );
        return StockReceiptImportWebMapper.toResponse(executeUseCase.execute(command));
    }
}
