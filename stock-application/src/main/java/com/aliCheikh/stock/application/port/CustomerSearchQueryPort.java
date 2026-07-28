package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.CustomerSearchView;

import java.util.List;

/**
 * Recherche de clients pour le sélecteur de vente.
 *
 * <p>Port de requête : on lit un modèle plat destiné à l'affichage, sans reconstruire les
 * agrégats. La recherche est faite côté base et non côté client, pour ne pas dépendre du
 * volume de clients.</p>
 */
public interface CustomerSearchQueryPort {

    /**
     * Clients dont le nom, le nom du père ou le numéro correspond au mot-clé.
     *
     * @param keyword terme de recherche, déjà nettoyé par le use case
     * @param limit   nombre maximum de résultats
     */
    List<CustomerSearchView> findCustomersByKeyword(String keyword, int limit);

    /** Les clients les plus récemment enregistrés, pour amorcer le sélecteur sans saisie. */
    List<CustomerSearchView> findMostRecent(int limit);
}
