package com.aliCheikh.stock.domain.model.user;

import com.aliCheikh.stock.domain.exception.user.InvalidUserNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserTest {
    @Test
    public void should_create_user_with_valid_data() {
        // GIVEN
        UserId validId = UserId.generate();
        String validName = "Jean Dupont";
        UserRole validRole = UserRole.SELLER;

        // WHEN : On crée l'utilisateur
        User user = new User(validId, validName, validRole);

        // THEN : L'objet est bien créé et les getters renvoient les bonnes valeurs
        assertThat(user.getUserId()).isEqualTo(validId);
        assertThat(user.getUserName()).isEqualTo(validName);
        assertThat(user.getUserRole()).isEqualTo(validRole);
    }


    @Test
    public void should_throw_exception_when_creating_user_with_blank_name() {
        assertThatThrownBy(() -> {
            User user = new User(UserId.generate(), "  ", UserRole.OWNER);
        }).isInstanceOf(InvalidUserNameException.class).extracting(ex -> (InvalidUserNameException) ex).satisfies(ex -> {
            assertThat(ex.getInvalidUserName()).isEqualTo("  ");
        });
    }
}
