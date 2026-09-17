package com.aliCheikh.stock.domain.service;

import com.aliCheikh.stock.domain.exception.user.LastActiveOwnerException;
import com.aliCheikh.stock.domain.model.user.User;
import com.aliCheikh.stock.domain.model.user.UserEmail;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerContinuityTest {

    private final OwnerContinuity ownerContinuity = new OwnerContinuity();

    @Test
    void the_last_active_owner_cannot_be_deactivated() {
        User owner = User.newOwner("Patron", UserEmail.of("owner@example.com"));

        assertThatThrownBy(() -> ownerContinuity.ensureDeactivationKeepsAnActiveOwner(owner, 1))
                .isInstanceOf(LastActiveOwnerException.class)
                .hasMessage("The shop must retain at least one active owner.");
    }

    @Test
    void an_owner_can_be_deactivated_when_another_owner_remains_active() {
        User owner = User.newOwner("Patron", UserEmail.of("owner@example.com"));

        assertThatCode(() -> ownerContinuity.ensureDeactivationKeepsAnActiveOwner(owner, 2))
                .doesNotThrowAnyException();
    }

    @Test
    void deactivating_a_seller_does_not_depend_on_the_number_of_owners() {
        User seller = User.newSeller("Vendeur", UserEmail.of("seller@example.com"));

        assertThatCode(() -> ownerContinuity.ensureDeactivationKeepsAnActiveOwner(seller, 1))
                .doesNotThrowAnyException();
    }
}
