package com.aliCheikh.stock.domain.model.product.port;

import com.aliCheikh.stock.domain.model.category.CategoryId;
import com.aliCheikh.stock.domain.model.product.Product;
import com.aliCheikh.stock.domain.model.product.ProductId;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<Product> findById(ProductId product);

    Optional<Product> findByReference(String reference);

    void save(Product product);

    List<Product> findAll(int page, int size);

    long count();

    List<Product> findAllActive(int page, int size);

    long countActive();

    boolean existsByName(String name);

    /**
     * Indique si au moins un produit est rangé dans cette catégorie.
     *
     * <p>La question porte sur les produits, elle appartient donc à leur port : une catégorie ne
     * connaît pas ce qui la référence.</p>
     */
    boolean existsByCategoryId(CategoryId categoryId);
}
