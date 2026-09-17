package com.aliCheikh.stock.domain.model.stock;

import com.aliCheikh.stock.domain.exception.stock.InsufficientStockException;
import com.aliCheikh.stock.domain.exception.stock.InvalidStockOperationException;
import com.aliCheikh.stock.domain.model.product.ProductId;

import java.util.Objects;

public final class StockLevel {
    private final ProductId productId;
    private final int quantity;

    private StockLevel(ProductId productId, int qty) {
        Objects.requireNonNull(productId, "productId cannot be null");

        if (qty < 0) {
            // Report zero as the current quantity before the object exists.
            throw new InvalidStockOperationException(productId, 0, qty);
        }

        this.productId = productId;
        this.quantity = qty;
    }

    public static StockLevel of(ProductId productId, int quantity) {
        return new StockLevel(productId, quantity);
    }

    public StockLevel decrease(int quantityToDecrease) {
        if (quantityToDecrease <= 0) {

            throw new InvalidStockOperationException(productId, this.quantity, quantityToDecrease);
        }

        if (quantityToDecrease > quantity) {
            throw new InsufficientStockException(productId, quantity, quantityToDecrease);
        }

        return new StockLevel(productId, quantity - quantityToDecrease);
    }

    public StockLevel increase(int quantityToIncrease) {
        if (quantityToIncrease <= 0) {
            // Ici aussi
            throw new InvalidStockOperationException(productId, this.quantity, quantityToIncrease);
        }
        return new StockLevel(productId, quantity + quantityToIncrease);
    }

    public ProductId getProductId() {
        return productId;
    }


    public int getQuantity() {
        return quantity;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockLevel stockLevel = (StockLevel) o;
        return productId.equals(stockLevel.productId) && quantity == stockLevel.quantity;
    }


    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity);
    }


}
