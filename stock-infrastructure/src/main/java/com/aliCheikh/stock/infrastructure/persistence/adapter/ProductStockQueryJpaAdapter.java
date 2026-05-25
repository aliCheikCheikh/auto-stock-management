package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.GetProductStockLevelsQuery;
import com.aliCheikh.stock.application.dto.ProductStockSummaryView;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.port.ProductStockQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.infrastructure.persistence.entity.ProductJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.projection.ProductStockLevelRow;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductJpaRepository;
import com.aliCheikh.stock.infrastructure.persistence.repository.ProductStockQueryJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class ProductStockQueryJpaAdapter implements ProductStockQueryPort {

    private final ProductStockQueryJpaRepository productStockQueryJpaRepository;
    private final ProductJpaRepository productRepository;

    public ProductStockQueryJpaAdapter(
            ProductStockQueryJpaRepository productStockQueryJpaRepository,
            ProductJpaRepository productRepository
    ) {
        this.productStockQueryJpaRepository = Objects.requireNonNull(
                productStockQueryJpaRepository,
                "productStockQueryJpaRepository cannot be null"
        );
        this.productRepository = Objects.requireNonNull(
                productRepository,
                "productRepository cannot be null"
        );
    }

    @Override
    public Optional<ProductStockSummaryView> findProductStockSummary(GetProductStockLevelsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        return productRepository.findById(query.productId().getValue())
                .map(product -> toSummary(product, query));
    }

    private ProductStockSummaryView toSummary(
            ProductJpaEntity product,
            GetProductStockLevelsQuery query
    ) {
        List<ProductStockLevelRow> rows = productStockQueryJpaRepository.findStockLevelsByProductId(
                query.productId().getValue(),
                query.shopId() == null ? null : query.shopId().getValue()
        );

        int globalQuantity = rows.stream()
                .mapToInt(ProductStockLevelRow::quantity)
                .sum();

        return new ProductStockSummaryView(
                ProductId.of(product.getId()),
                product.getName(),
                globalQuantity,
                product.getMinimumGlobalThreshold(),
                globalQuantity < product.getMinimumGlobalThreshold(),
                rows.stream()
                        .map(this::toStockLevelView)
                        .toList()
        );
    }

    private StockLevelView toStockLevelView(ProductStockLevelRow row) {
        return new StockLevelView(
                ProductId.of(row.productId()),
                row.productName(),
                LocationId.of(row.locationId()),
                row.locationName(),
                row.locationType(),
                ShopId.of(row.shopId()),
                row.quantity()
        );
    }
}