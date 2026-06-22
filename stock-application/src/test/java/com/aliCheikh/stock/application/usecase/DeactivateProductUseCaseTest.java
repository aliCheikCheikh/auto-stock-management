package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.domain.exception.product.ProductNotFoundException;
import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeactivateProductUseCaseTest {

    private ProductRepository productRepository;
    private DeactivateProductUseCase deactivateProductUseCase;
    private ProductId productId;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = ProductId.generate();
        Money unitPrice = Money.create(new BigDecimal("15.00"), Currency.getInstance("EUR"));
        product = new Product(
                productId,
                "Product 1",
                "PRD-001",
                CategoryId.generate(),
                4,
                unitPrice
        );
        productRepository = mock(ProductRepository.class);
        deactivateProductUseCase = new DeactivateProductUseCase(productRepository);
    }

    @Test
    void should_deactivate_product_when_product_exists() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        deactivateProductUseCase.execute(productId);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().isActive()).isFalse();
    }

    @Test
    void should_throw_exception_when_product_is_not_found() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivateProductUseCase.execute(productId))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any());
    }
}
