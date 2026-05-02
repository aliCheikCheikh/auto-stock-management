package com.aliCheikh.stock.domain.model.movement.port;

import com.aliCheikh.stock.domain.model.movement.StockMovement;

import java.util.List;

public interface StockMovementRepository {
    void save(StockMovement stockMovement);

    void saveAll(List<StockMovement> stockMovements);
}
