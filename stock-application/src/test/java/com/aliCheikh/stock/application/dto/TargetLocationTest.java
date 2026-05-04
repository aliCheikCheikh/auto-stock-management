package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.stock.LocationId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class TargetLocationTest {

    private final LocationId locationId = LocationId.generate();

    @Test
    void should_create_target_location_when_parameters_are_valid() {
        assertDoesNotThrow(() ->
                new TargetLocation(locationId, 5)
        );
    }

    @Test
    void should_reject_null_location_id() {
        assertThatThrownBy(() ->
                new TargetLocation(null, 5)
        )
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("locationId cannot be null");
    }

    @Test
    void should_reject_zero_quantity() {
        assertThatThrownBy(() ->
                new TargetLocation(locationId, 0)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be strictly positive");
    }

    @Test
    void should_reject_negative_quantity() {
        assertThatThrownBy(() ->
                new TargetLocation(locationId, -1)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity must be strictly positive");
    }
}
