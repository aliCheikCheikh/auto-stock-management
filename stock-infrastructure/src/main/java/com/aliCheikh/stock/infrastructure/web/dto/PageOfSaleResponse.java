package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;

public record PageOfSaleResponse(
        List<SaleResponse> content,
        PageMetaResponse page
) {
}
