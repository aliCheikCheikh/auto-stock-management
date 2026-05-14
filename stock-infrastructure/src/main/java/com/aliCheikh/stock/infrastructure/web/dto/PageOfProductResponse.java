package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;

public record PageOfProductResponse(List<ProductResponse> content,
                                    PageMetaResponse page) {
}
