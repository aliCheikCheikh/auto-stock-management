package com.aliCheikh.stock.domain.exception.movement;

public enum MovementErrorReason {
    ENTRY_MISSING_DESTINATION,
    EXIT_MISSING_SOURCE,
    TRANSFER_SAME_SOURCE_DESTINATION
}
