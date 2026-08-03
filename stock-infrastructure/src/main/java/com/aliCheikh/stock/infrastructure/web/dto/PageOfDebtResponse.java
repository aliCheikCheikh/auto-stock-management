package com.aliCheikh.stock.infrastructure.web.dto;

import java.util.List;

/**
 * Une page de créances.
 *
 * <p>L'historique des créances soldées ne cesse de croître : au bout de deux ans d'activité, une
 * réponse non paginée renverrait des milliers de lignes que personne ne lira. La pagination n'est
 * pas un confort ici, c'est ce qui empêche l'écran de devenir inutilisable avec le succès de la
 * boutique.</p>
 */
public record PageOfDebtResponse(List<DebtResponse> content,
                                 PageMetaResponse page) {
}
