package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.stock.LocationId;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;
import com.aliCheikh.stock.infrastructure.persistence.entity.StorageLocationJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.mapper.StorageLocationJpaMapper;
import com.aliCheikh.stock.infrastructure.persistence.repository.StorageLocationJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class StorageLocationJpaRepositoryAdapter implements StorageLocationRepository {

    private final StorageLocationJpaRepository storageLocationJpaRepository;
    private final StorageLocationJpaMapper storageLocationJpaMapper;

    public StorageLocationJpaRepositoryAdapter(
            StorageLocationJpaRepository storageLocationJpaRepository,
            StorageLocationJpaMapper storageLocationJpaMapper
    ) {
        this.storageLocationJpaRepository = Objects.requireNonNull(
                storageLocationJpaRepository,
                "storageLocationJpaRepository cannot be null"
        );
        this.storageLocationJpaMapper = Objects.requireNonNull(
                storageLocationJpaMapper,
                "storageLocationJpaMapper cannot be null"
        );
    }

    @Override
    public List<StorageLocation> findByShopId(ShopId shopId) {
        Objects.requireNonNull(shopId, "shopId cannot be null");

        return storageLocationJpaRepository.findByShopId(shopId.getValue())
                .stream()
                .map(storageLocationJpaMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<StorageLocation> findById(LocationId locationId) {
        Objects.requireNonNull(locationId, "locationId cannot be null");

        return storageLocationJpaRepository.findById(locationId.getValue())
                .map(storageLocationJpaMapper::toDomain);
    }

    @Override
    public void save(StorageLocation storageLocation) {
        Objects.requireNonNull(storageLocation, "storageLocation cannot be null");
        Optional<StorageLocationJpaEntity> existingEntity = storageLocationJpaRepository
                .findById(storageLocation.getLocationId().getValue());
        if(existingEntity.isPresent()) {
            StorageLocationJpaEntity existing = existingEntity.get();
            storageLocationJpaMapper.updateEntity(storageLocation, existing);
            storageLocationJpaRepository.save(existing);
            return;
        }

        storageLocationJpaRepository.save(storageLocationJpaMapper.toEntity(storageLocation));
    }

    @Override
    public List<StorageLocation> findAll() {
        return storageLocationJpaRepository.findAll()
                .stream()
                .map(storageLocationJpaMapper::toDomain)
                .toList();
    }

    @Override
    public void saveAll(List<StorageLocation> storageLocations) {
        Objects.requireNonNull(storageLocations, "storageLocations cannot be null");

        storageLocationJpaRepository.saveAll(
                storageLocations.stream()
                        .map(storageLocationJpaMapper::toEntity)
                        .toList()
        );
    }
}