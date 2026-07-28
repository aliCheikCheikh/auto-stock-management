package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.RegisterCustomerCommand;
import com.aliCheikh.stock.application.usecase.ListOutstandingDebtsUseCase;
import com.aliCheikh.stock.application.usecase.RegisterCustomerUseCase;
import com.aliCheikh.stock.domain.exception.customer.DuplicatePhoneNumberException;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;
import com.aliCheikh.stock.domain.model.customer.port.CustomerRepository;
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
import static org.mockito.Mockito.verify;
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
    private ListOutstandingDebtsUseCase listOutstandingDebtsUseCase;

    @MockitoBean
    private CustomerRepository customerRepository;

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
}
