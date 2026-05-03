package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SellProductCommandTest {

    private final UserId sellerId = UserId.generate();
    private final ShopId shopId = ShopId.generate();
    private final List<SellLineCommand> lines = List.of(
            new SellLineCommand(ProductId.generate(), 2)
    );

    @Test
    void should_create_command_when_parameters_are_valid() {
        assertDoesNotThrow(() ->
                new SellProductCommand(sellerId, shopId, lines)
        );
    }

    @Test
    void should_reject_null_seller_id() {
        assertThatThrownBy(() ->
                new SellProductCommand(null, shopId, lines)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sellerId cannot be null");
    }

    @Test
    void should_reject_null_shop_id() {
        assertThatThrownBy(() ->
                new SellProductCommand(sellerId, null, lines)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("shopId cannot be null");
    }

    @Test
    void should_reject_null_lines() {
        assertThatThrownBy(() ->
                new SellProductCommand(sellerId, shopId, null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("lines cannot be null");
    }

    @Test
    void should_reject_empty_lines() {
        assertThatThrownBy(() ->
                new SellProductCommand(sellerId, shopId, List.of())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one line is required");
    }
}