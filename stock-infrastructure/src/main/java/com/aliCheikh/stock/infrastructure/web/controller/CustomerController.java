package com.aliCheikh.stock.infrastructure.web.controller;

import com.aliCheikh.stock.application.dto.RegisterCustomerCommand;
import com.aliCheikh.stock.application.usecase.ListOutstandingDebtsUseCase;
import com.aliCheikh.stock.application.usecase.RegisterCustomerUseCase;
import com.aliCheikh.stock.domain.exception.customer.CustomerNotFoundException;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.port.CustomerRepository;
import com.aliCheikh.stock.infrastructure.web.dto.CreateCustomerRequest;
import com.aliCheikh.stock.infrastructure.web.dto.CustomerResponse;
import com.aliCheikh.stock.infrastructure.web.dto.OutstandingDebtResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final ListOutstandingDebtsUseCase listOutstandingDebtsUseCase;
    private final CustomerRepository customerRepository;

    public CustomerController(RegisterCustomerUseCase registerCustomerUseCase,
                              ListOutstandingDebtsUseCase listOutstandingDebtsUseCase,
                              CustomerRepository customerRepository) {
        this.registerCustomerUseCase = Objects.requireNonNull(registerCustomerUseCase);
        this.listOutstandingDebtsUseCase = Objects.requireNonNull(listOutstandingDebtsUseCase);
        this.customerRepository = Objects.requireNonNull(customerRepository);
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

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID customerId) {
        CustomerId id = CustomerId.of(customerId);

        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    /** Les créances en cours d'un client donné. */
    @GetMapping("/{customerId}/debts")
    public List<OutstandingDebtResponse> getCustomerDebts(@PathVariable UUID customerId) {
        return listOutstandingDebtsUseCase.listByCustomer(customerId).stream()
                .map(OutstandingDebtResponse::from)
                .toList();
    }
}
