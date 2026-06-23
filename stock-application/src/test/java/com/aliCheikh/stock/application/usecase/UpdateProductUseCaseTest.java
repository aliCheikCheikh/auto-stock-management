package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.UpdateProductCommand;
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
import static org.mockito.Mockito.*;

class UpdateProductUseCaseTest {
    private ProductRepository productRepository;
    private UpdateProductUseCase updateProductUseCase;
    private ProductId productId;
    private Product product;
    private UpdateProductCommand updateProductCommand;
    private String updatedName;
    private Money updatedPrice;
    private int updatedThreshold;


    @BeforeEach
    void setUp() {
        productId = ProductId.generate();
        CategoryId categoryId = CategoryId.generate();
        Money unitPrice = Money.create(new BigDecimal("15.00"), Currency.getInstance("EUR"));
        updatedPrice = Money.create(new BigDecimal("10.00"), Currency.getInstance("EUR"));
        product = new Product(
                productId,
                "Product 1",
                "PRD-001",
                categoryId,
                4,
                unitPrice
        );
        updatedName = "Updated Product Name";
        updatedThreshold = 5;
        updateProductCommand = new UpdateProductCommand(
                productId,
                updatedName,
                updatedPrice,
                updatedThreshold

        );
        productRepository = mock(ProductRepository.class);
        updateProductUseCase = new UpdateProductUseCase(productRepository);
    }

    @Test
    void should_update_name_price_and_threshold_when_product_exists() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        updateProductUseCase.execute(updateProductCommand);
        verify(productRepository, times(1)).findById(productId);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());
        Product updatedProduct = productCaptor.getValue();
        assertThat(updatedProduct.getName()).isEqualTo(updatedName);
        assertThat(updatedProduct.getUnitPrice().getAmount()).isEqualTo(updatedPrice.getAmount());
        assertThat(updatedProduct.getUnitPrice().getCurrency()).isEqualTo(updatedPrice.getCurrency());
        assertThat(updatedProduct.getMinimumGlobalThreshold()).isEqualTo(updatedThreshold);

    }

    @Test
    void should_throw_exception_when_product_is_not_found() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> updateProductUseCase.execute(updateProductCommand))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).save(any());
    }
}
