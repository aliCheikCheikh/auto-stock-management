package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.OutstandingDebtView;

import java.util.List;
import java.util.UUID;

/**
 * Lecture des créances (ventes dont le solde reste dû).
 *
 * <p>Port de <b>requête</b> et non de repository : on lit un modèle plat destiné à l'affichage,
 * sans reconstruire les agrégats — inutile et coûteux pour une liste.</p>
 */
public interface OutstandingDebtQueryPort {

    /** Toutes les créances en cours, de la plus ancienne à la plus récente. */
    List<OutstandingDebtView> findAllOutstanding();

    /** Les créances en cours d'un client donné. */
    List<OutstandingDebtView> findOutstandingByCustomer(UUID customerId);
}
