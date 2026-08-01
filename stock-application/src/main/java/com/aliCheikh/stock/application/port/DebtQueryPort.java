package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.DebtView;
import com.aliCheikh.stock.application.dto.ListDebtsQuery;
import com.aliCheikh.stock.application.dto.PageResult;

/**
 * Lecture des ventes à crédit, en cours comme éteintes.
 *
 * <p>Port de <b>requête</b> et non de repository : on lit un modèle plat destiné à l'affichage,
 * sans reconstruire les agrégats — inutile et coûteux pour une liste.</p>
 *
 * <p>Une seule méthode plutôt qu'une par combinaison de critères : le statut et le client sont des
 * filtres, pas des cas d'usage distincts, et les décliner en méthodes obligerait à en ajouter une
 * à chaque nouveau critère.</p>
 */
public interface DebtQueryPort {

    /**
     * @return la page demandée, dans l'ordre que le statut impose : de la plus ancienne à la plus
     * récente pour les créances en cours, du règlement le plus récent au plus ancien pour les
     * créances éteintes
     */
    PageResult<DebtView> findByQuery(ListDebtsQuery query);
}
