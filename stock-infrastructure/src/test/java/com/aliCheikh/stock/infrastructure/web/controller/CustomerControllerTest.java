package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.DebtStatus;
import com.aliCheikh.stock.application.dto.DebtSummary;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.RegisterCustomerCommand;
import com.aliCheikh.stock.application.usecase.ListDebtsUseCase;
import com.aliCheikh.stock.application.usecase.GetCustomerUseCase;
import com.aliCheikh.stock.application.usecase.RegisterCustomerUseCase;
import com.aliCheikh.stock.application.usecase.SearchCustomersUseCase;
import com.aliCheikh.stock.domain.exception.customer.DuplicatePhoneNumberException;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.repository.IdempotencyRecordJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterCustomerUseCase registerCustomerUseCase;

    @MockitoBean
    private ListDebtsUseCase listDebtsUseCase;

    @MockitoBean
    private GetCustomerUseCase getCustomerUseCase;

    @MockitoBean
    private SearchCustomersUseCase searchCustomersUseCase;

    /** Requis par IdempotencyFilter, chargé par la tranche web mais dépendant de la persistance. */
    @MockitoBean
    private IdempotencyRecordJpaRepository idempotencyRecordJpaRepository;

    @Test
    void should_create_a_customer_and_return_its_canonical_phone_number() throws Exception {
        Customer created = Customer.create(
                CustomerId.generate(), PhoneNumber.of("66 12 34 56"), "Ahmat", "Youssouf", null);
        given(registerCustomerUseCase.register(any())).willReturn(created);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "givenName": "Ahmat",
                                  "fatherName": "Youssouf",
                                  "phoneNumber": "66 12 34 56"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.givenName").value("Ahmat"))
                .andExpect(jsonPath("$.phoneNumber").value("+23566123456"));

        // Le téléphone est transmis brut : sa normalisation appartient au domaine, pas au web.
        ArgumentCaptor<RegisterCustomerCommand> captor =
                ArgumentCaptor.forClass(RegisterCustomerCommand.class);
        verify(registerCustomerUseCase).register(captor.capture());
        assertThat(captor.getValue().rawPhoneNumber()).isEqualTo("66 12 34 56");
    }

    @Test
    void should_reject_a_customer_without_given_name() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "givenName": "  ",
                                  "phoneNumber": "66 12 34 56"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_409_when_the_phone_number_is_already_used() throws Exception {
        given(registerCustomerUseCase.register(any()))
                .willThrow(new DuplicatePhoneNumberException(PhoneNumber.of("66 12 34 56")));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "givenName": "Ahmat",
                                  "phoneNumber": "66 12 34 56"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOMER_PHONE_ALREADY_USED"));
    }

    /**
     * La fiche d'un client pose la même question que l'écran global, sur un périmètre plus étroit.
     * Elle doit donc filtrer de la même façon — et se restreindre au client de l'URL, faute de quoi
     * elle exposerait les dettes de toute la boutique sur la fiche d'un seul.
     */
    @Test
    void should_scope_the_debts_to_the_customer_in_the_path() throws Exception {
        UUID customerId = UUID.randomUUID();
        given(listDebtsUseCase.execute(any())).willReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/customers/{customerId}/debts", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        ArgumentCaptor<ListDebtsQuery> captor = ArgumentCaptor.forClass(ListDebtsQuery.class);
        verify(listDebtsUseCase).execute(captor.capture());

        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
        assertThat(captor.getValue().status()).isEqualTo(DebtStatus.OUTSTANDING);
    }

    @Test
    void should_let_the_owner_read_a_customer_repayment_history() throws Exception {
        UUID customerId = UUID.randomUUID();
        LocalDateTime settledAt = LocalDateTime.of(2026, 7, 28, 9, 15);

        given(listDebtsUseCase.execute(any())).willReturn(new PageResult<>(
                List.of(new DebtSummary(
                        UUID.randomUUID(),
                        LocalDateTime.of(2026, 7, 20, 10, 30),
                        customerId,
                        "Ahmat",
                        "Youssouf",
                        "+23566123456",
                        Money.create(new BigDecimal("50000"), Currency.getInstance("XAF")),
                        Money.create(new BigDecimal("50000"), Currency.getInstance("XAF")),
                        Money.create(BigDecimal.ZERO, Currency.getInstance("XAF")),
                        true,
                        settledAt,
                        8,
                        false)),
                0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/customers/{customerId}/debts", customerId)
                        .param("status", "SETTLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].settled").value(true))
                .andExpect(jsonPath("$.content[0].settledAt").value("2026-07-28T09:15:00"));

        ArgumentCaptor<ListDebtsQuery> captor = ArgumentCaptor.forClass(ListDebtsQuery.class);
        verify(listDebtsUseCase).execute(captor.capture());

        assertThat(captor.getValue().status()).isEqualTo(DebtStatus.SETTLED);
        assertThat(captor.getValue().customerId()).isEqualTo(customerId);
    }
}
