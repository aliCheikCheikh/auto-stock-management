package com.aliCheikh.stock.domain.model.stock;

public enum LocationType {
    SHOP_FLOOR(1),
    BACKSTOCK(2);

    private final int priority;

    LocationType(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
