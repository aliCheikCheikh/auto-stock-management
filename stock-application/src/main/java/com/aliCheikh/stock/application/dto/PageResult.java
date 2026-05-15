package com.aliCheikh.stock.application.dto;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PageResult {
        content = List.copyOf(Objects.requireNonNull(content, "content cannot be null"));
    }
}
