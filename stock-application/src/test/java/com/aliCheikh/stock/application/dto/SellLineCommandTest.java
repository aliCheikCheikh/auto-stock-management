package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SellLineCommandTest {

    private final ProductId productId = ProductId.generate();

    @Test
    void should_create_sell_line_when_parameters_are_valid() {
        assertDoesNotThrow(() ->
                new SellLineCommand(productId, 3)
        );
    }

    @Test
    void should_reject_null_product_id() {
        assertThatThrownBy(() ->
                new SellLineCommand(null, 3)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("productId cannot be null");
    }

    @Test
    void should_reject_zero_quantity() {
        assertThatThrownBy(() ->
                new SellLineCommand(productId, 0)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be strictly positive");
    }

    @Test
    void should_reject_negative_quantity() {
        assertThatThrownBy(() ->
                new SellLineCommand(productId, -1)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be strictly positive");
    }
}