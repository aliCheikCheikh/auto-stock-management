package com.aliCheikh.stock.domain.model.sale;

import com.aliCheikh.stock.domain.exception.sale.InvalidSaleException;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SaleTest {

    @Test
    public void should_throw_exception_when_creating_sale_without_lines() {
        // GIVEN
        UserId sellerId = UserId.generate();
        List<SaleLineInput> emptyLines = new ArrayList<>();

        // WHEN & THEN
        assertThatThrownBy(() -> Sale.create(sellerId, emptyLines))
                .isInstanceOf(InvalidSaleException.class)
                .extracting(ex -> (InvalidSaleException) ex)
                .satisfies(ex -> {
                    // Verifying the exact message thrown by our Sale aggregate
                    assertThat(ex.getMessage()).contains("at least one line item");
                });
    }

    @Test
    public void should_calculate_total_amount_correctly_when_creating_sale() {
        // GIVEN: A seller and two items (2 filters at 15 EUR, 1 oil can at 30 EUR)
        UserId sellerId = UserId.generate();
        Currency eur = Currency.getInstance("EUR");

        List<SaleLineInput> requests = List.of(
                new SaleLineInput(ProductId.generate(), 2, Money.create(BigDecimal.valueOf(15), eur)), // Line total: 30 EUR
                new SaleLineInput(ProductId.generate(), 1, Money.create(BigDecimal.valueOf(30), eur))  // Line total: 30 EUR
        );

        // WHEN: Creating the sale
        Sale sale = Sale.create(sellerId, requests);

        // THEN: The total must be exactly 60 EUR
        Money expectedTotal = Money.create(BigDecimal.valueOf(60), eur);

        assertThat(sale.getTotalAmount()).isEqualTo(expectedTotal);
        assertThat(sale.getSoldBy()).isEqualTo(sellerId);
        assertThat(sale.getLines()).hasSize(2);
        assertThat(sale.getOccurredAt()).isNotNull();
        assertThat(sale.getSaleId()).isNotNull();
    }
}