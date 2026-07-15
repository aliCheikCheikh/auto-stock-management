package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.ContextLocationView;
import com.aliCheikh.stock.application.dto.ContextView;
import com.aliCheikh.stock.infrastructure.web.dto.ContextLocationResponse;
import com.aliCheikh.stock.infrastructure.web.dto.ContextResponse;

public final class ContextWebMapper {

    private ContextWebMapper() {
    }

    public static ContextResponse toResponse(ContextView view) {
        return new ContextResponse(
                view.shopId().getValue(),
                view.locations().stream()
                        .map(ContextWebMapper::toLocationResponse)
                        .toList()
        );
    }

    private static ContextLocationResponse toLocationResponse(ContextLocationView view) {
        return new ContextLocationResponse(
                view.type().name(),
                view.locationId().getValue(),
                view.label()
        );
    }
}
