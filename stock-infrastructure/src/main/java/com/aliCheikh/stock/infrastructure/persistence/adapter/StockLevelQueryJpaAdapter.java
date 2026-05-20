package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.ListStockLevelsQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.StockLevelView;
import com.aliCheikh.stock.application.port.StockLevelQueryPort;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.infrastructure.persistence.projection.StockLevelRow;
import com.aliCheikh.stock.infrastructure.persistence.repository.StockLevelQueryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class StockLevelQueryJpaAdapter implements StockLevelQueryPort {

    private final StockLevelQueryJpaRepository stockLevelQueryJpaRepository;

    public StockLevelQueryJpaAdapter(StockLevelQueryJpaRepository stockLevelQueryJpaRepository) {
        this.stockLevelQueryJpaRepository = Objects.requireNonNull(
                stockLevelQueryJpaRepository,
                "stockLevelQueryJpaRepository cannot be null"
        );
    }

    @Override
    public PageResult<StockLevelView> findByQuery(ListStockLevelsQuery query) {
        Objects.requireNonNull(query, "query cannot be null");

        Page<StockLevelRow> page = stockLevelQueryJpaRepository.findByQuery(
                query.productId() == null ? null : query.productId().getValue(),
                query.shopId() == null ? null : query.shopId().getValue(),
                query.locationId() == null ? null : query.locationId().getValue(),
                Boolean.TRUE.equals(query.belowThreshold()),
                PageRequest.of(query.page(), query.size())
        );

        return new PageResult<>(
                page.getContent().stream()
                        .map(this::toView)
                        .toList(),
                query.page(),
                query.size(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private StockLevelView toView(StockLevelRow row) {
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
