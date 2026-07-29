package com.aliCheikh.stock.domain.model.sale.port;

import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleId;

import java.util.Optional;

public interface SaleRepository {

    void save(Sale sale);

    Optional<Sale> findById(SaleId saleId);

    /**
     * Charge une vente en vue de la modifier, en garantissant un accès exclusif jusqu'à la fin de
     * la transaction en cours.
     *
     * <p>Ce n'est pas un détail d'implémentation mais un <b>contrat</b> : encaisser suppose de lire
     * le solde puis d'écrire un paiement. Sans exclusivité, deux règlements simultanés liraient le
     * même solde, se croiraient tous deux dans les limites du montant dû, et la vente finirait
     * sur-payée.</p>
     */
    Optional<Sale> findByIdForUpdate(SaleId saleId);
}
