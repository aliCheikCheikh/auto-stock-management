package com.aliCheikh.stock.domain.model.stock.ports;

import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;

import java.util.List;
import java.util.Optional;

public interface StorageLocationRepository {
    List<StorageLocation> findByShopId(ShopId shopId);
    Optional<StorageLocation> findById(LocationId locationId);
    void save(StorageLocation storageLocation);
    List<StorageLocation> findAll();
    void saveAll(List<StorageLocation> storageLocations);
}
