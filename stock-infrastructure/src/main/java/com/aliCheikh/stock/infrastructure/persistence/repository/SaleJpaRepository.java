package com.aliCheikh.stock.infrastructure.persistence.repository;

import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * <p>Lignes et paiements sont chargés dans la même requête : tous deux sont nécessaires à la
     * reconstruction de l'agrégat, et les récupérer séparément provoquerait un N+1.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"saleLines", "payments"})
    @Query("SELECT s FROM SaleJpaEntity s WHERE s.id = :saleId")
    Optional<SaleJpaEntity> findByIdForUpdate(@Param("saleId") UUID saleId);
}
