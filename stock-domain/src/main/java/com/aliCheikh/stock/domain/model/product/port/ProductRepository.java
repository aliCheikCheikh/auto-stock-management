package com.aliCheikh.stock.domain.model.product.port;

import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;

import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(ProductId product);

    Optional<Product> findByReference(String reference);

    void save(Product product);
}
