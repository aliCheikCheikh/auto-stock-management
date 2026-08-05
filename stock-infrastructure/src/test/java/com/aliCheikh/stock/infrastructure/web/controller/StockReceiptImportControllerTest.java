package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionCommand;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionReport;
import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionSummary;
import com.aliCheikh.stock.application.dto.StockReceiptImportPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowAction;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowExecutionResult;
import com.aliCheikh.stock.application.dto.StockReceiptImportRowPreview;
import com.aliCheikh.stock.application.dto.StockReceiptImportSummary;
import com.aliCheikh.stock.application.dto.TargetLocation;
import com.aliCheikh.stock.application.exception.InvalidStockReceiptImportFileException;
import com.aliCheikh.stock.application.exception.StockReceiptImportExecutionConflictException;
import com.aliCheikh.stock.application.exception.StockReceiptImportFileErrorCode;
import com.aliCheikh.stock.application.usecase.ExecuteStockReceiptImportUseCase;
import com.aliCheikh.stock.application.usecase.PrepareStockReceiptImportUseCase;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockReceiptImportController.class)
@AutoConfigureMockMvc(addFilters = false)
class StockReceiptImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @MockitoBean
    private PrepareStockReceiptImportUseCase prepareUseCase;

    @MockitoBean
    private ExecuteStockReceiptImportUseCase executeUseCase;

    private UUID shopId;
    private UUID userId;
    private UUID importId;
    private UUID productId;
    private UUID categoryId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        shopId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        importId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        productId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        categoryId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        locationId = UUID.fromString("66666666-6666-6666-6666-666666666666");
    }

    @Test
    void downloads_an_excel_friendly_empty_csv_template() throws Exception {
        byte[] response = mockMvc.perform(get("/api/v1/stock-receipts/import-template"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"modele-import-produits.csv\""
                ))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(response).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(response, StandardCharsets.UTF_8)).isEqualTo(
                "\uFEFFreference;nom_produit;categorie;prix_unitaire_xaf;seuil_alerte;"
                        + "quantite_surface;quantite_reserve\r\n"
        );
    }

    @Test
    void previews_the_file_without_executing_an_import() throws Exception {
        given(prepareUseCase.execute(any(), any())).willReturn(preview());

        mockMvc.perform(multipart("/api/v1/stock-receipts/import-preview")
                        .file(csvUpload())
                        .param("shopId", shopId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalRows").value(1))
                .andExpect(jsonPath("$.summary.productsToCreate").value(1))
                .andExpect(jsonPath("$.rows[0].lineNumber").value(2))
                .andExpect(jsonPath("$.rows[0].action").value("CREATE_PRODUCT"))
                .andExpect(jsonPath("$.rows[0].categoryName").value("Moteur"))
                .andExpect(jsonPath("$.rows[0].unitPrice.amount").value("12500"))
                .andExpect(jsonPath("$.rows[0].distributions[0].quantity").value(10));

        ArgumentCaptor<com.aliCheikh.stock.application.dto.StockReceiptImportFile> fileCaptor =
                ArgumentCaptor.forClass(com.aliCheikh.stock.application.dto.StockReceiptImportFile.class);
        verify(prepareUseCase).execute(fileCaptor.capture(), org.mockito.ArgumentMatchers.eq(ShopId.of(shopId)));
        assertThat(fileCaptor.getValue().filename()).isEqualTo("produits.csv");
        assertThat(fileCaptor.getValue().content()).isEqualTo(csvContent());
        verifyNoInteractions(executeUseCase);
    }

    @Test
    void executes_only_selected_lines_with_the_authenticated_user() throws Exception {
        StockReceiptImportRowExecutionResult imported = StockReceiptImportRowExecutionResult.imported(
                2,
                StockReceiptImportRowAction.CREATE_PRODUCT,
                "REF-001",
                "Filtre à huile",
                ProductId.of(productId),
                10
        );
        given(executeUseCase.execute(any())).willReturn(new StockReceiptImportExecutionReport(
                importId,
                List.of(imported),
                StockReceiptImportExecutionSummary.from(List.of(imported), 1)
        ));

        mockMvc.perform(multipart("/api/v1/stock-receipts/import-executions")
                        .file(csvUpload())
                        .param("shopId", shopId.toString())
                        .param("importId", importId.toString())
                        .param("selectedLineNumbers", "2")
                        .principal(authenticatedAs(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importId").value(importId.toString()))
                .andExpect(jsonPath("$.summary.importedRows").value(1))
                .andExpect(jsonPath("$.summary.totalQuantityReceived").value(10))
                .andExpect(jsonPath("$.rows[0].status").value("IMPORTED"))
                .andExpect(jsonPath("$.rows[0].productId").value(productId.toString()));

        ArgumentCaptor<StockReceiptImportExecutionCommand> commandCaptor =
                ArgumentCaptor.forClass(StockReceiptImportExecutionCommand.class);
        verify(executeUseCase).execute(commandCaptor.capture());
        StockReceiptImportExecutionCommand command = commandCaptor.getValue();
        assertThat(command.importId()).isEqualTo(importId);
        assertThat(command.shopId()).isEqualTo(ShopId.of(shopId));
        assertThat(command.userId()).isEqualTo(UserId.of(userId));
        assertThat(command.selectedLineNumbers()).containsExactly(2);
    }

    @Test
    void rejects_a_file_larger_than_one_megabyte_before_the_use_case() throws Exception {
        MockMultipartFile upload = new MockMultipartFile(
                "file",
                "trop-grand.csv",
                "text/csv",
                new byte[1024 * 1024 + 1]
        );

        mockMvc.perform(multipart("/api/v1/stock-receipts/import-preview")
                        .file(upload)
                        .param("shopId", shopId.toString()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));

        verifyNoInteractions(prepareUseCase, executeUseCase);
    }

    @Test
    void exposes_a_stable_code_when_the_csv_headers_are_invalid() throws Exception {
        given(prepareUseCase.execute(any(), any())).willThrow(new InvalidStockReceiptImportFileException(
                StockReceiptImportFileErrorCode.INVALID_HEADER,
                "Les colonnes du fichier ne correspondent pas au modèle attendu."
        ));

        mockMvc.perform(multipart("/api/v1/stock-receipts/import-preview")
                        .file(csvUpload())
                        .param("shopId", shopId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Fichier d'import invalide"))
                .andExpect(jsonPath("$.code").value("INVALID_HEADER"));
    }

    @Test
    void reports_an_import_identifier_conflict_without_reprocessing() throws Exception {
        given(executeUseCase.execute(any()))
                .willThrow(new StockReceiptImportExecutionConflictException(importId));

        mockMvc.perform(multipart("/api/v1/stock-receipts/import-executions")
                        .file(csvUpload())
                        .param("shopId", shopId.toString())
                        .param("importId", importId.toString())
                        .param("selectedLineNumbers", "2")
                        .principal(authenticatedAs(userId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IMPORT_ID_REUSED"))
                .andExpect(jsonPath("$.importId").value(importId.toString()));
    }

    private StockReceiptImportPreview preview() {
        StockReceiptImportRowPreview row = new StockReceiptImportRowPreview(
                2,
                StockReceiptImportRowAction.CREATE_PRODUCT,
                "REF-001",
                "Filtre à huile",
                "Moteur",
                CategoryId.of(categoryId),
                Money.create(new BigDecimal("12500"), Currency.getInstance("XAF")),
                5,
                null,
                List.of(new TargetLocation(LocationId.of(locationId), 10)),
                List.of()
        );
        return new StockReceiptImportPreview(
                List.of(row),
                new StockReceiptImportSummary(1, 1, 0, 0)
        );
    }

    private MockMultipartFile csvUpload() {
        return new MockMultipartFile("file", "produits.csv", "text/csv", csvContent());
    }

    private byte[] csvContent() {
        return ("reference;nom_produit;categorie;prix_unitaire_xaf;seuil_alerte;"
                + "quantite_surface;quantite_reserve\n"
                + "REF-001;Filtre à huile;Moteur;12500;5;10;0\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private UsernamePasswordAuthenticationToken authenticatedAs(UUID authenticatedUserId) {
        return new UsernamePasswordAuthenticationToken(authenticatedUserId, null, List.of());
    }
}
