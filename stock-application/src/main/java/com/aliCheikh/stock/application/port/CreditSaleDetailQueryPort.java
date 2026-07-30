package com.aliCheikh.stock.application.port;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;

import java.util.Optional;
import java.util.UUID;

/**
 * Lecture du détail d'une vente à crédit.
 *
 * <p>Port de <b>requête</b> : on assemble un modèle d'affichage — produits nommés, vendeur nommé,
 * encaissements — sans reconstruire l'agrégat, qui ne porte que des identifiants.</p>
 */
public interface CreditSaleDetailQueryPort {

    /**
     * @return le détail, ou vide si la vente n'existe pas <b>ou</b> n'a pas de client. Une vente au
     * comptant n'est pas une créance : elle n'a rien à faire sur cet écran.
     */
    Optional<CreditSaleDetailView> findCreditSaleDetail(UUID saleId);
}
