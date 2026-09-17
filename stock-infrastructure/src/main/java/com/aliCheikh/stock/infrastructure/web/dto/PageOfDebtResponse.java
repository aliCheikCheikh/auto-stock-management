package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;

/** Paginated outstanding and settled debts. */
public record PageOfDebtResponse(List<DebtResponse> content,
                                 PageMetaResponse page) {
}
