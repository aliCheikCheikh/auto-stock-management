package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SaleJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {

    /**
     * Charge une vente en verrouillant sa ligne jusqu'à la fin de la transaction.
     *
     * <p>Nécessaire pour encaisser : sans ce verrou, deux règlements simultanés liraient le même
     * solde, chacun se croirait dans les limites du montant dû, et la vente finirait sur-payée.
     * Le verrou pessimiste sérialise les encaissements d'une même vente — sémantique exacte
     * recherchée, et sans coût réel ici, les règlements concurrents sur une même dette étant
     * rarissimes dans une boutique à un comptoir.</p>
     *
     * <p>Les collections {@code saleLines} et {@code payments} ne doivent pas être jointes dans
     * cette requête. Une jointure simultanée produit une ligne SQL par couple ligne de vente ×
     * paiement ; comme les paiements sont ordonnés dans une {@link java.util.List}, Hibernate les
     * duplique alors en mémoire et fausse le solde. Le mapper initialise les deux collections par
     * deux lectures secondaires, toujours dans la transaction qui porte ce verrou. Il ne s'agit
     * pas d'un N+1 : une seule vente est chargée par encaissement.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SaleJpaEntity s WHERE s.id = :saleId")
    Optional<SaleJpaEntity> findByIdForUpdate(@Param("saleId") UUID saleId);
}
