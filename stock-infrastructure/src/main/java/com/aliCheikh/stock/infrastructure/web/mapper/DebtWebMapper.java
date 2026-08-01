package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.DebtSummary;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.infrastructure.web.dto.DebtResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfDebtResponse;

/**
 * Passage du modèle de lecture des créances à sa forme HTTP.
 *
 * <p>Deux contrôleurs exposent des créances — la vue globale et celle d'un client. Le mapping vit
 * ici plutôt que dans chacun d'eux, pour qu'ils ne puissent pas répondre différemment à la même
 * question.</p>
 */
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
