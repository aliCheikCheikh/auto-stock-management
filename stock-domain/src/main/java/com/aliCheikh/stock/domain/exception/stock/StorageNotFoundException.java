package com.aliCheikh.stock.domain.exception.stock;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.stock.LocationId;

public class StorageNotFoundException extends DomainException {
    private final LocationId locationId;
    public StorageNotFoundException(LocationId locationId) {
        super("Storage not found for locationId: " + locationId);
        this.locationId = locationId;
    }
    public LocationId getLocationId() {
        return locationId;
    }
}
