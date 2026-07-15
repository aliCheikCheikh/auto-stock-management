package com.aliCheikh.stock.application.usecase;

import com.aliCheikh.stock.application.dto.ContextLocationView;
import com.aliCheikh.stock.application.dto.ContextView;
import com.aliCheikh.stock.domain.model.stock.StorageLocation;
import com.aliCheikh.stock.domain.model.stock.ports.StorageLocationRepository;

import java.util.List;
import java.util.Objects;

/**
 * Fournit le contexte de session : le magasin courant et ses emplacements.
 *
 * <p>Hypothèse mono-magasin assumée : tous les emplacements appartiennent au
 * seul magasin, donc {@code findAll()} suffit à reconstruire tout le contexte
 * (le magasin est celui des emplacements). Le jour où plusieurs magasins seront
 * un vrai besoin, seul ce use case (et la résolution du magasin courant) évolue.</p>
 */
public class GetSessionContextUseCase {

    private final StorageLocationRepository storageLocationRepository;

    public GetSessionContextUseCase(StorageLocationRepository storageLocationRepository) {
        this.storageLocationRepository = Objects.requireNonNull(
                storageLocationRepository, "storageLocationRepository cannot be null");
    }

    public ContextView execute() {
        List<StorageLocation> locations = storageLocationRepository.findAll();

        if (locations.isEmpty()) {
            throw new IllegalStateException(
                    "No storage location provisioned; cannot build the session context");
        }

        List<ContextLocationView> locationViews = locations.stream()
                .map(location -> new ContextLocationView(
                        location.getLocationType(),
                        location.getLocationId(),
                        location.getLabel()))
                .toList();

        return new ContextView(locations.get(0).getShopId(), locationViews);
    }
}
