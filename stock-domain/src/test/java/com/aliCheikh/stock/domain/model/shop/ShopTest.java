package com.aliCheikh.stock.domain.model.shop;

import com.aliCheikh.stock.domain.exception.shop.InvalidShopNameException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ShopTest {
    @Test
    public void should_create_shop_with_valid_data() {
        //GIVEN
        ShopId shopId = ShopId.generate();
        String name = "Magasin";
        String address = "Ndjamena";

        //WHEN
        Shop shop = new Shop(shopId, name, address);

        //THEN
        assertThat(shop.getName()).isEqualTo(name);
        assertThat(shop.getAddress()).isEqualTo(address);
        assertThat(shop.getShopId()).isEqualTo(shopId);


    }


    @Test
    public void should_throw_exception_when_creating_shop_with_empty_or_blank_name() {
        assertThatThrownBy(() -> {
            Shop shop = new Shop(ShopId.generate(), "  ", "Ndjamena");
        }).isInstanceOf(InvalidShopNameException.class).extracting(ex -> (InvalidShopNameException) ex).satisfies(ex -> {
            assertThat(ex.getInvalidShopName()).isEqualTo("  ");
        });
    }
}
