package com.aliCheikh.stock.domain.model.product;

import com.aliCheikh.stock.domain.exception.product.InvalidProductNameException;
import com.aliCheikh.stock.domain.exception.product.InvalidProductPriceException;
import com.aliCheikh.stock.domain.exception.product.InvalidThresholdException;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private Money validPrice;
    private Money invalidPrice;
    private Product product;

    @BeforeEach
    void setUp() {
        validPrice = Money.create(BigDecimal.valueOf(15), Currency.getInstance("EUR"));
        invalidPrice = Money.create(BigDecimal.valueOf(-1), Currency.getInstance("EUR"));
        product = new Product(ProductId.generate(),
                "filtre à huile",
                "FH-TOY-2024-001",
                CategoryId.generate(),
                8,
                validPrice);
    }

    @Test
    void should_throw_exception_when_creating_product_with_zero_or_negative_price() {
        assertThatThrownBy(() -> {
            Product product = new Product(ProductId.generate(), "filtre à huile", "FH-TOY-2024-001", CategoryId.generate(), 3, invalidPrice);
        }).isInstanceOf(InvalidProductPriceException.class)
                .extracting(ex -> (InvalidProductPriceException) ex)
                .satisfies(ex -> {
                    // CORRECTION ICI : Utilisation de invalidPrice
                    assertThat(ex.getInvalidPrice()).isEqualTo(invalidPrice);
                });
    }

    @Test
    void should_throw_exception_when_creating_product_with_empty_name() {
        assertThatThrownBy(() -> {
            Product product = new Product(ProductId.generate(), " ", "FH-TOY-2024-001", CategoryId.generate(), 3, validPrice);
        }).isInstanceOf(InvalidProductNameException.class)
                .extracting(ex -> (InvalidProductNameException) ex)
                .satisfies(ex -> {
                    assertThat(ex.getInvalidProductName()).isEqualTo(" ");
                });
    }

    @Test
    void should_throw_exception_when_creating_product_with_negative_threshold() {
        assertThatThrownBy(() -> {
            Product product = new Product(ProductId.generate(), "filtre à huile", "FH-TOY-2024-001", CategoryId.generate(), -3, validPrice);
        }).isInstanceOf(InvalidThresholdException.class)
                .extracting(ex -> (InvalidThresholdException) ex)
                .satisfies(ex -> {
                    assertThat(ex.getInvalidThreshold()).isEqualTo(-3);
                });
    }

    @Test
    void should_return_true_when_is_below_threshold() {
        assertThat(product.isGlobalStockBelowThreshold(5)).isTrue();   // En dessous
        assertThat(product.isGlobalStockBelowThreshold(10)).isFalse(); // Au dessus
        assertThat(product.isGlobalStockBelowThreshold(8)).isFalse();  // Sur la limite exacte (Boundary test)
    }


    @Test
    void should_be_active_by_default_when_created() {
        assertThat(product.isActive()).isTrue();
    }

    @Test
    void should_be_inactive_when_deactivated() {
        product.deactivate();
        assertThat(product.isActive()).isFalse();
    }

    @Test
    void should_restore_inactive_product() {
        com.aliCheikh.stock.domain.model.product.Product restoredProduct = Product.restore(product.getProductId(),
                product.getName(),
                product.getReference(),
                product.getCategoryId(),
                product.getMinimumGlobalThreshold(),
                product.getUnitPrice(),
                false
        );
        assertThat(restoredProduct.isActive()).isFalse();
    }

}