package com.aliCheikh.stock.infrastructure.web.dto;

public record PageMetaResponse(int page,
                               int size,
                               long totalElements,
                               int totalPages) {
}
