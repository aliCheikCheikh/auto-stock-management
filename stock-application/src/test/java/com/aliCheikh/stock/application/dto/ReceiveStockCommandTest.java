package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ReceiveStockCommandTest {

    private final String productReference = "REF-123";
    private final ShopId shopId = ShopId.generate();
    private final UserId userId = UserId.generate();
    private final LocationId shopFloorId = LocationId.generate();
    private final LocationId backStockId = LocationId.generate();

    private final List<TargetLocation> distributions = List.of(
            new TargetLocation(shopFloorId, 15),
            new TargetLocation(backStockId, 35)
    );

    @Test
    void should_create_command_when_parameters_are_valid() {
        assertDoesNotThrow(() ->
                new ReceiveStockCommand(productReference, null, shopId, userId, distributions)
        );
    }

    @Test
    void should_reject_null_product_reference() {
        assertThatThrownBy(() ->
                new ReceiveStockCommand(null, null, shopId, userId, distributions)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("productReference cannot be null");
    }

    @Test
    void should_reject_blank_product_reference() {
        assertThatThrownBy(() ->
                new ReceiveStockCommand(" ", null, shopId, userId, distributions)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productReference cannot be blank");
    }

    @Test
    void should_reject_null_shop_id() {
        assertThatThrownBy(() ->
                new ReceiveStockCommand(productReference, null, null, userId, distributions)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("shopId cannot be null");
    }

    @Test
    void should_reject_null_user_id() {
        assertThatThrownBy(() ->
                new ReceiveStockCommand(productReference, null, shopId, null, distributions)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId cannot be null");
    }

    @Test
    void should_reject_null_distributions() {
        assertThatThrownBy(() ->
                new ReceiveStockCommand(productReference, null, shopId, userId, null)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("distributions cannot be null");
    }

    @Test
    void should_reject_empty_distributions() {
        assertThatThrownBy(() ->
                new ReceiveStockCommand(productReference, null, shopId, userId, List.of())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distributions cannot be empty");
    }

    @Test
    void should_reject_duplicate_target_locations() {
        List<TargetLocation> duplicateDistributions = List.of(
                new TargetLocation(shopFloorId, 15),
                new TargetLocation(shopFloorId, 35)
        );

        assertThatThrownBy(() ->
                new ReceiveStockCommand(productReference, null, shopId, userId, duplicateDistributions)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distributions cannot contain duplicate locationId");
    }
}
