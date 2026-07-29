package com.aliCheikh.stock.domain.model.user;

import com.aliCheikh.stock.domain.exception.user.InvalidUserNameException;
import com.aliCheikh.stock.domain.exception.user.OwnerPasswordResetNotAllowedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void a_new_seller_is_active_and_must_change_the_temporary_password() {
        User seller = User.newSeller("  Amina Mahamat  ", UserEmail.of("AMINA@EXAMPLE.COM"));

        assertThat(seller.getDisplayName()).isEqualTo("Amina Mahamat");
        assertThat(seller.getEmail().getValue()).isEqualTo("amina@example.com");
        assertThat(seller.getRole()).isEqualTo(UserRole.SELLER);
        assertThat(seller.isActive()).isTrue();
        assertThat(seller.isPasswordChangeRequired()).isTrue();
    }

    @Test
    void a_display_name_is_required() {
        assertThatThrownBy(() -> User.newSeller("  ", UserEmail.of("seller@example.com")))
                .isInstanceOf(InvalidUserNameException.class);
    }

    @Test
    void a_user_can_be_renamed_without_changing_the_login_email() {
        UserEmail email = UserEmail.of("seller@example.com");
        User seller = User.newSeller("Ancien nom", email);

        seller.rename("Nouveau nom");

        assertThat(seller.getDisplayName()).isEqualTo("Nouveau nom");
        assertThat(seller.getEmail()).isEqualTo(email);
    }

    @Test
    void a_deactivated_user_can_be_reactivated() {
        User seller = User.newSeller("Amina", UserEmail.of("seller@example.com"));

        seller.deactivate();
        assertThat(seller.isActive()).isFalse();

        seller.reactivate();
        assertThat(seller.isActive()).isTrue();
    }

    @Test
    void changing_the_temporary_password_clears_the_requirement() {
        User seller = User.newSeller("Amina", UserEmail.of("seller@example.com"));

        seller.confirmPasswordChange();

        assertThat(seller.isPasswordChangeRequired()).isFalse();
    }

    @Test
    void an_owner_password_cannot_be_reset_from_user_management() {
        User owner = User.newOwner("Patron", UserEmail.of("owner@example.com"));

        assertThatThrownBy(owner::requirePasswordChange)
                .isInstanceOf(OwnerPasswordResetNotAllowedException.class);
    }
}
