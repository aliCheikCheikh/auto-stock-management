package com.aliCheikh.stock.domain.model.stock;

import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockOperationException;
import com.aliCheikh.stock.domain.model.product.ProductId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class StockLevelTest {
    @Test
    public void should_throw_insufficient_stock_exception_when_decreasing_below_zero() {
        int initialQuantity = 5;
        int quantityToDecrease = 10;
        ProductId productId = ProductId.generate(); // J'utilise ta factory
        StockLevel stock = StockLevel.of(productId, initialQuantity);

        assertThatThrownBy(() -> stock.decrease(quantityToDecrease))
                .isInstanceOf(InsufficientStockException.class)
                .extracting(ex -> (InsufficientStockException) ex)
                .satisfies(ex -> {

                    assertThat(ex.getProductId()).isEqualTo(productId);
                    assertThat(ex.getAvailableQuantity()).isEqualTo(initialQuantity);
                    assertThat(ex.getRequestedQuantity()).isEqualTo(quantityToDecrease);
                });
    }

    @Test
    public void should_decrease_quantity_when_sufficient_stock() {
        int initialQuantity = 5;
        int quantityToDecrease = 2;
        ProductId productId = ProductId.generate();
        StockLevel stock = StockLevel.of(productId, initialQuantity);

        StockLevel newStock = stock.decrease(quantityToDecrease);


        assertThat(newStock.getQuantity()).isEqualTo(initialQuantity - quantityToDecrease);
        assertThat(newStock.getProductId()).isEqualTo(productId);

        // The original value remains unchanged.
        assertThat(stock.getQuantity()).isEqualTo(initialQuantity);
    }


    @Test
    public void should_throw_invalid_increasing_value_exception_when_adding_negative_quantity() {
        int initialQuantity = 10;
        int quantityToIncrease = -1;
        ProductId productId = ProductId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        StockLevel stock = StockLevel.of(productId, initialQuantity);
        assertThatThrownBy(() -> stock.increase(quantityToIncrease)).isInstanceOf(InvalidStockOperationException.class).extracting(ex -> (InvalidStockOperationException) ex).satisfies(ex -> {
            assertThat(ex.getProductId()).isEqualTo(productId);
            assertThat(ex.getCurrentQuantity()).isEqualTo(initialQuantity);
            assertThat(ex.getInvalidQuantity()).isEqualTo(quantityToIncrease);
        });
    }

    @Test
    public void should_increase_quantity_when_adding_valid_amount() {
        int initialQuantity = 10;
        int quantityToIncrease = 1;
        ProductId productId = ProductId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        StockLevel stock = StockLevel.of(productId, initialQuantity);
        StockLevel newStock = stock.increase(quantityToIncrease);
        assertThat(newStock.getQuantity()).isEqualTo(initialQuantity + quantityToIncrease);
        assertThat(newStock.getProductId()).isEqualTo(productId);
        assertThat(stock.getQuantity()).isEqualTo(initialQuantity);
    }
}
