package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.CustomerSearchView;
import com.aliCheikh.stock.application.port.CustomerSearchQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class SearchCustomersUseCaseTest {

    private CustomerSearchQueryPort queryPort;
    private SearchCustomersUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = mock(CustomerSearchQueryPort.class);
        useCase = new SearchCustomersUseCase(queryPort);
    }

    @Test
    public void should_return_most_recent_customers_when_no_keyword_is_given() {
        given(queryPort.findMostRecent(anyInt())).willReturn(List.of(view("Ahmat")));

        assertThat(useCase.search(null)).hasSize(1);
        assertThat(useCase.search("   ")).hasSize(1);

        verify(queryPort, never()).findCustomersByKeyword(anyString(), anyInt());
    }

    @Test
    public void should_ignore_a_keyword_too_short_to_be_discriminating() {
        assertThat(useCase.search("a")).isEmpty();

        verify(queryPort, never()).findCustomersByKeyword(anyString(), anyInt());
    }

    @Test
    public void should_normalize_a_keyword_that_is_a_phone_number() {
        // Normalize the search input to match the stored phone number.
        useCase.search("66 12 34 56");

        ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        verify(queryPort).findCustomersByKeyword(keyword.capture(), anyInt());
        assertThat(keyword.getValue()).isEqualTo("+23566123456");
    }

    @Test
    public void should_pass_a_name_keyword_through_untouched() {
        useCase.search("Ahmat");

        ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        verify(queryPort).findCustomersByKeyword(keyword.capture(), anyInt());
        assertThat(keyword.getValue()).isEqualTo("Ahmat");
    }

    @Test
    public void should_keep_a_partial_phone_number_as_a_plain_keyword() {
        // A partial phone number must remain searchable even when it cannot be normalized.
        useCase.search("6612");

        ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        verify(queryPort).findCustomersByKeyword(keyword.capture(), anyInt());
        assertThat(keyword.getValue()).isEqualTo("6612");
    }

    private static CustomerSearchView view(String givenName) {
        return new CustomerSearchView(UUID.randomUUID(), givenName, null, "+23566123456");
    }
}
