package com.aliCheikh.stock.infrastructure.web.controller;


import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.product.port.ProductRepository;
import com.aliCheikh.stock.domain.model.shared.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;


import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(ProductController.class)
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @Test
    void should_return_404_when_product_does_not_exist() throws Exception {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(productRepository.findById(ProductId.of(productId))).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/products/{productId}", productId)).andExpect(status().isNotFound());
    }

    @Test
    void should_return_200_when_product_exists() throws Exception {
        UUID productId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID categoryId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Product product = new Product(
                ProductId.of(productId),
                "Plaquettes de frein",
                "BRK-PAD-001",
                CategoryId.of(categoryId),
                10,
                Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"))
        );

        when(productRepository.findById(ProductId.of(productId))).thenReturn(Optional.of(product));
        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Plaquettes de frein"))
                .andExpect(jsonPath("$.reference").value("BRK-PAD-001"))
                .andExpect(jsonPath("$.categoryId").value(categoryId.toString()))
                .andExpect(jsonPath("$.minimumGlobalThreshold").value(10))
                .andExpect(jsonPath("$.unitPrice.amount").value("45.90"))
                .andExpect(jsonPath("$.unitPrice.currency").value("EUR"));

    }

    @Test
    void should_return_400_when_product_id_is_not_uuid() throws Exception {
        mockMvc.perform(get("/api/v1/products/not-uuid")).andExpect(status().isBadRequest());
        verifyNoInteractions(productRepository);
    }
}
