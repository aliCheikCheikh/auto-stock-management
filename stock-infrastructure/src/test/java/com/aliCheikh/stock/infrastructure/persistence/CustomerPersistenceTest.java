package com.aliCheikh.stock.infrastructure.persistence;

import com.aliCheikh.stock.domain.exception.customer.DuplicatePhoneNumberException;
import com.aliCheikh.stock.domain.model.customer.Customer;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.customer.PhoneNumber;
import com.aliCheikh.stock.infrastructure.persistence.adapter.CustomerJpaRepositoryAdapter;
import com.aliCheikh.stock.infrastructure.persistence.mapper.CustomerJpaMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.context.annotation.Import({
        CustomerJpaRepositoryAdapter.class,
        CustomerJpaMapper.class
})
class CustomerPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auto_stock_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CustomerJpaRepositoryAdapter customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void should_save_and_read_back_a_customer() {
        Customer customer = Customer.create(
                CustomerId.generate(),
                PhoneNumber.of("66 12 34 56"),
                "Ahmat",
                "Youssouf",
                "Ahmat@Example.COM");

        customerRepository.save(customer);
        entityManager.flush();
        entityManager.clear();

        Optional<Customer> reloaded = customerRepository.findById(customer.getCustomerId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getGivenName()).isEqualTo("Ahmat");
        assertThat(reloaded.get().getFatherName()).contains("Youssouf");
        // Le téléphone est relu sous sa forme canonique, l'email en minuscules.
        assertThat(reloaded.get().getPhoneNumber()).isEqualTo(PhoneNumber.of("66 12 34 56"));
        assertThat(reloaded.get().getEmail()).contains("ahmat@example.com");
    }

    @Test
    void should_reject_two_customers_sharing_the_same_phone_number() {
        customerRepository.save(Customer.create(
                CustomerId.generate(), PhoneNumber.of("66 12 34 56"), "Ahmat", null, null));

        // Même numéro, écrit autrement : après normalisation, c'est un doublon.
        Customer duplicate = Customer.create(
                CustomerId.generate(), PhoneNumber.of("+235 66 12 34 56"), "Moussa", null, null);

        // L'adapter traduit la violation de contrainte en exception métier : c'est ce qui permet
        // de répondre 409 plutôt que 500 lorsque deux créations concurrentes franchissent le
        // contrôle d'unicité préalable.
        assertThatThrownBy(() -> customerRepository.save(duplicate))
                .isInstanceOf(DuplicatePhoneNumberException.class)
                .hasMessageContaining("+23566123456");
    }

    @Test
    void should_allow_several_customers_without_email() {
        // Cas courant au Tchad : c'est ce que la contrainte UNIQUE doit tolérer,
        // les NULL étant distincts entre eux en PostgreSQL.
        customerRepository.save(Customer.create(
                CustomerId.generate(), PhoneNumber.of("66 12 34 56"), "Ahmat", null, null));
        customerRepository.save(Customer.create(
                CustomerId.generate(), PhoneNumber.of("77 12 34 56"), "Moussa", null, null));

        entityManager.flush();

        assertThat(customerRepository.existsByPhoneNumber(PhoneNumber.of("66 12 34 56"))).isTrue();
        assertThat(customerRepository.existsByPhoneNumber(PhoneNumber.of("99 99 99 99"))).isFalse();
    }

    @Test
    void should_find_a_customer_by_its_canonical_phone_number() {
        customerRepository.save(Customer.create(
                CustomerId.generate(), PhoneNumber.of("0023566123456"), "Ahmat", null, null));
        entityManager.flush();

        // Saisi dans un format différent, le numéro reste le même une fois normalisé.
        assertThat(customerRepository.existsByPhoneNumber(PhoneNumber.of("66 12 34 56"))).isTrue();
    }
}
