package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TransferStockCommandTest {

    private final ProductId productId = ProductId.generate();
    private final LocationId sourceLocationId = LocationId.generate();
    private final LocationId destinationLocationId = LocationId.generate();
    private final UserId userId = UserId.generate();

    @Test
    void should_create_command_when_all_parameters_are_valid() {
        assertDoesNotThrow(() ->
                new TransferStockCommand(productId, sourceLocationId, destinationLocationId, 5, userId)
        );
    }

    @Test
    void should_reject_null_product_id() {
        assertThatThrownBy(() ->
                new TransferStockCommand(null, sourceLocationId, destinationLocationId, 5, userId)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("productId cannot be null");
    }

    @Test
    void should_reject_null_source_location_id() {
        assertThatThrownBy(() ->
                new TransferStockCommand(productId, null, destinationLocationId, 5, userId)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sourceLocationId cannot be null");
    }

    @Test
    void should_reject_null_destination_location_id() {
        assertThatThrownBy(() ->
                new TransferStockCommand(productId, sourceLocationId, null, 5, userId)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("destinationLocationId cannot be null");
    }

    @Test
    void should_reject_null_user_id() {
        assertThatThrownBy(() ->
                new TransferStockCommand(productId, sourceLocationId, destinationLocationId, 5, null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId cannot be null");
    }

    @Test
    void should_reject_zero_quantity() {
        assertThatThrownBy(() ->
                new TransferStockCommand(productId, sourceLocationId, destinationLocationId, 0, userId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be strictly positive");
    }

    @Test
    void should_reject_negative_quantity() {
        assertThatThrownBy(() ->
                new TransferStockCommand(productId, sourceLocationId, destinationLocationId, -1, userId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be strictly positive");
    }

    @Test
    void should_reject_same_source_and_destination() {
        assertThatThrownBy(() ->
                new TransferStockCommand(productId, sourceLocationId, sourceLocationId, 5, userId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceLocationId and destinationLocationId cannot be the same");
    }
}