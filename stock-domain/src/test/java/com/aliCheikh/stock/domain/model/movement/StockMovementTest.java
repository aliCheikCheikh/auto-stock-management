package com.aliCheikh.stock.domain.model.movement;

import com.aliCheikh.stock.domain.exception.movement.InvalidMovementException;
import com.aliCheikh.stock.domain.exception.movement.InvalidQuantityMovementException;
import com.aliCheikh.stock.domain.exception.movement.MovementErrorReason;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class StockMovementTest {
    @Test
    public void should_throw_invalid_movement_exception_when_creating_entry_without_destination() {
        //GIVEN
        ProductId productId = ProductId.generate();
        LocationId destinationId = null;
        UserId userdId = UserId.generate();
        int quantity = 10;
        //WHEN & THEN
        assertThatThrownBy(() -> {
            StockMovement.createEntry(productId, destinationId, quantity, userdId);
        }).isInstanceOf(InvalidMovementException.class).extracting(ex -> ((InvalidMovementException) ex).getMovementErrorReason()).isEqualTo(MovementErrorReason.ENTRY_MISSING_DESTINATION);
    }

    @Test
    public void should_throw_invalid_movement_exception_when_creating_exit_without_source() {
        ProductId productId = ProductId.generate();
        LocationId sourceId = null;
        int quantity = 10;
        UserId userId = UserId.generate();
        SaleId saleId = SaleId.generate();
        assertThatThrownBy(() -> {
            StockMovement.createExit(productId, sourceId, quantity, userId, saleId);
        }).isInstanceOf(InvalidMovementException.class).extracting(ex -> ((InvalidMovementException) ex).getMovementErrorReason()).isEqualTo(MovementErrorReason.EXIT_MISSING_SOURCE);
    }


    @Test
    public void should_throw_invalid_movement_exception_when_transfer_source_equals_destination() {
        LocationId sameLocationId = LocationId.generate();
        ProductId productId = ProductId.generate();
        LocationId sourceId = sameLocationId;
        LocationId destinationId = sameLocationId;
        int quantity = 10;
        UserId userId = UserId.generate();
        assertThatThrownBy(() -> {
            StockMovement.createTransfer(productId, sourceId, destinationId, quantity, userId);
        }).isInstanceOf((InvalidMovementException.class)).extracting(ex -> ((InvalidMovementException) ex).getMovementErrorReason()).isEqualTo(MovementErrorReason.TRANSFER_SAME_SOURCE_DESTINATION);

    }

    @Test
    public void should_throw_exception_when_quantity_is_negative_or_zero() {
        ProductId productId = ProductId.generate();
        LocationId destinationId = LocationId.generate();
        int invalidQuantity = -5;
        UserId userId = UserId.generate();
        assertThatThrownBy(() -> {
            StockMovement.createEntry(productId, destinationId, invalidQuantity, userId);
        }).isInstanceOf(InvalidQuantityMovementException.class).hasMessageContaining("must be strictly positive").extracting(ex -> ((InvalidQuantityMovementException) ex).getFaultQuantity()).isEqualTo(invalidQuantity);

    }


}
