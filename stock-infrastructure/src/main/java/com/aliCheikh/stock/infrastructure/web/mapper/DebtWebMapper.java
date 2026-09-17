package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.DebtSummary;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.infrastructure.web.dto.DebtResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfDebtResponse;

/** Shared mapping for global and customer-specific debt endpoints. */
public final class DebtWebMapper {

    private DebtWebMapper() {
    }

    public static PageOfDebtResponse toPageResponse(PageResult<DebtSummary> page) {
        return new PageOfDebtResponse(
                page.content().stream().map(DebtResponse::from).toList(),
                new PageMetaResponse(
                        page.page(),
                        page.size(),
                        page.totalElements(),
                        page.totalPages()));
    }
}
