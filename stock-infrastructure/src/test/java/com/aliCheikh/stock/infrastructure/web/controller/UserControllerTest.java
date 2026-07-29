package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.CreateSellerCommand;
import com.aliCheikh.stock.application.dto.CreatedUser;
import com.aliCheikh.stock.application.dto.TemporaryPassword;
import com.aliCheikh.stock.application.usecase.CreateSellerUseCase;
import com.aliCheikh.stock.application.usecase.DeactivateUserUseCase;
import com.aliCheikh.stock.application.usecase.ListUsersUseCase;
import com.aliCheikh.stock.application.usecase.ReactivateUserUseCase;
import com.aliCheikh.stock.application.usecase.RenameUserUseCase;
import com.aliCheikh.stock.application.usecase.ResetSellerPasswordUseCase;
import com.aliCheikh.stock.domain.exception.user.LastActiveOwnerException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateSellerUseCase createSellerUseCase;

    @MockitoBean
    private ListUsersUseCase listUsersUseCase;

    @MockitoBean
    private RenameUserUseCase renameUserUseCase;

    @MockitoBean
    private DeactivateUserUseCase deactivateUserUseCase;

    @MockitoBean
    private ReactivateUserUseCase reactivateUserUseCase;

    @MockitoBean
    private ResetSellerPasswordUseCase resetSellerPasswordUseCase;

    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @Test
    void creating_a_seller_returns_the_generated_password_once_without_caching() throws Exception {
        User seller = User.newSeller("Amina Mahamat", UserEmail.of("amina@example.com"));
        given(createSellerUseCase.execute(any()))
                .willReturn(new CreatedUser(seller, "ABCD-EFGH-JKLM-NPQR"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Amina Mahamat","email":"amina@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.user.displayName").value("Amina Mahamat"))
                .andExpect(jsonPath("$.user.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.temporaryPassword").value("ABCD-EFGH-JKLM-NPQR"));

        ArgumentCaptor<CreateSellerCommand> command = ArgumentCaptor.forClass(CreateSellerCommand.class);
        verify(createSellerUseCase).execute(command.capture());
        assertThat(command.getValue().displayName()).isEqualTo("Amina Mahamat");
        assertThat(command.getValue().email()).isEqualTo("amina@example.com");
    }

    @Test
    void invalid_creation_is_rejected_before_calling_the_use_case() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void resetting_a_password_returns_a_non_cacheable_temporary_password() throws Exception {
        User seller = User.newSeller("Amina", UserEmail.of("amina@example.com"));
        given(resetSellerPasswordUseCase.execute(seller.getId()))
                .willReturn(new TemporaryPassword(seller, "ABCD-EFGH-JKLM-NPQR"));

        mockMvc.perform(post("/api/v1/users/{id}/reset-password", seller.getId().getValue()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.temporaryPassword").value("ABCD-EFGH-JKLM-NPQR"));
    }

    @Test
    void deactivating_the_last_owner_returns_an_explicit_conflict() throws Exception {
        User owner = User.newOwner("Patron", UserEmail.of("owner@example.com"));
        willThrow(new LastActiveOwnerException()).given(deactivateUserUseCase).execute(owner.getId());

        mockMvc.perform(delete("/api/v1/users/{id}", owner.getId().getValue()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ACTIVE_OWNER"));
    }
}
