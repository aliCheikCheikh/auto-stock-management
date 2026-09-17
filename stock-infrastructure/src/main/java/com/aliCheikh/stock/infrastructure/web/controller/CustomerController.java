package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.DebtStatus;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.RegisterCustomerCommand;
import com.aliCheikh.stock.application.usecase.GetCustomerUseCase;
import com.aliCheikh.stock.application.usecase.ListDebtsUseCase;
import com.aliCheikh.stock.application.usecase.RegisterCustomerUseCase;
import com.aliCheikh.stock.application.usecase.SearchCustomersUseCase;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.infrastructure.web.dto.CreateCustomerRequest;
import com.aliCheikh.stock.infrastructure.web.dto.CustomerResponse;
import com.aliCheikh.stock.infrastructure.web.dto.CustomerSummaryResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfDebtResponse;
import com.aliCheikh.stock.infrastructure.web.mapper.DebtWebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Customer HTTP endpoints backed by application use cases. */
@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final SearchCustomersUseCase searchCustomersUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final ListDebtsUseCase listDebtsUseCase;

    public CustomerController(RegisterCustomerUseCase registerCustomerUseCase,
                              SearchCustomersUseCase searchCustomersUseCase,
                              GetCustomerUseCase getCustomerUseCase,
                              ListDebtsUseCase listDebtsUseCase) {
        this.registerCustomerUseCase = Objects.requireNonNull(registerCustomerUseCase);
        this.searchCustomersUseCase = Objects.requireNonNull(searchCustomersUseCase);
        this.getCustomerUseCase = Objects.requireNonNull(getCustomerUseCase);
        this.listDebtsUseCase = Objects.requireNonNull(listDebtsUseCase);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = registerCustomerUseCase.register(new RegisterCustomerCommand(
                request.phoneNumber(),
                request.givenName(),
                request.fatherName(),
                request.email()));

        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer));
    }

    /** Searches customers by name or phone; returns recent customers when no keyword is supplied. */
    @GetMapping
    public List<CustomerSummaryResponse> searchCustomers(@RequestParam(required = false) String search) {
        return searchCustomersUseCase.search(search).stream()
                .map(CustomerSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable UUID customerId) {
        return CustomerResponse.from(getCustomerUseCase.byId(CustomerId.of(customerId)));
    }

    /**
     * Lists a customer's debts with the same status filters and pagination as the global debt
     * view.
     */
    @GetMapping("/{customerId}/debts")
    public PageOfDebtResponse getCustomerDebts(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "OUTSTANDING") DebtStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size
    ) {
        return DebtWebMapper.toPageResponse(
                listDebtsUseCase.execute(new ListDebtsQuery(page, size, status, customerId)));
    }
}
