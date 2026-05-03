package com.aliCheikh.stock.domain.exception.stock;

import com.aliCheikh.stock.domain.exception.DomainException;
import com.aliCheikh.stock.domain.model.stock.LocationId;

public class InvalidStockTransferException extends DomainException {
    private final LocationId sourceLocation;
    private final LocationId destinationLocation;
    private final InvalidStockTransferReason reason;

    public InvalidStockTransferException(LocationId sourceLocation, LocationId destinationLocation, InvalidStockTransferReason reason) {
        super(String.format("Invalid stock transfer from %s to %s: %s", sourceLocation, destinationLocation, reason));
        this.sourceLocation = sourceLocation;
        this.destinationLocation = destinationLocation;
        this.reason = reason;
    }

    public LocationId getSourceLocationId() {
        return sourceLocation;
    }

    public LocationId getDestinationLocationId() {
        return destinationLocation;
    }

    public InvalidStockTransferReason getReason() {
        return reason;
    }

}
