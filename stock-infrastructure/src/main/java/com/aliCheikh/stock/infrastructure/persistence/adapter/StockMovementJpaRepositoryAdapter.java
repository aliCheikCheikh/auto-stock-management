package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.model.movement.StockMovement;
import com.aliCheikh.stock.domain.model.movement.port.StockMovementRepository;
import com.aliCheikh.stock.infrastructure.persistence.mapper.StockMovementJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.StockMovementJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public class StockMovementJpaRepositoryAdapter implements StockMovementRepository {

    private final StockMovementJpaRepository stockMovementJpaRepository;
    private final StockMovementJpaMapper stockMovementJpaMapper;

    public StockMovementJpaRepositoryAdapter(
            StockMovementJpaRepository stockMovementJpaRepository,
            StockMovementJpaMapper stockMovementJpaMapper
    ) {
        this.stockMovementJpaRepository = Objects.requireNonNull(
                stockMovementJpaRepository,
                "stockMovementJpaRepository cannot be null"
        );
        this.stockMovementJpaMapper = Objects.requireNonNull(
                stockMovementJpaMapper,
                "stockMovementJpaMapper cannot be null"
        );
    }

    @Override
    public void save(StockMovement stockMovement) {
        Objects.requireNonNull(stockMovement, "stockMovement cannot be null");

        stockMovementJpaRepository.save(stockMovementJpaMapper.toEntity(stockMovement));
    }

    @Override
    public void saveAll(List<StockMovement> stockMovements) {
        Objects.requireNonNull(stockMovements, "stockMovements cannot be null");

        stockMovementJpaRepository.saveAll(
                stockMovements.stream()
                        .map(stockMovementJpaMapper::toEntity)
                        .toList()
        );
    }
}
