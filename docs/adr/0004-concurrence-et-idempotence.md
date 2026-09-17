# 0004 — Verrou optimiste sur le stock, pessimiste sur les paiements, clé d’idempotence

**Statut :** appliquée

## Contexte

Trois risques différents existent :

1. deux ventes simultanées prélèvent le même stock ;
2. deux encaissements simultanés lisent le même ancien solde et dépassent ensemble le reste dû ;
3. une requête est renvoyée après une coupure réseau (double clic, réseau instable) et crée un doublon.

## Décision

- **Stock** : verrou optimiste (`@Version`) sur les emplacements. Un conflit fait échouer la transaction au lieu d’écraser une modification.
- **Paiements** : verrou pessimiste (`PESSIMISTIC_WRITE`) sur la vente pendant l’encaissement, pour sérialiser les versements.
- **Rejeu** : en-tête `Idempotency-Key` sur les POST de vente, de réception et de transfert. Une réponse réussie est rejouée ; un corps différent avec la même clé est refusé.

## Conséquences

- Le verrou pessimiste peut faire attendre un second encaissement ; c’est acceptable avec quelques utilisateurs.
- Le verrou optimiste produit un conflit que l’utilisateur doit relancer.
- La clé d’idempotence est optionnelle côté API : elle protège les clients qui l’envoient, elle ne garantit pas une exécution unique dans tous les cas.

## Dans le code

- [StorageLocationJpaEntity](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/persistence/entity/StorageLocationJpaEntity.java) (`@Version`)
- [SaleJpaRepository.findByIdForUpdate](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/persistence/repository/SaleJpaRepository.java), [RecordPaymentUseCase](../../stock-application/src/main/java/com/aliCheikh/stock/application/usecase/RecordPaymentUseCase.java)
- [IdempotencyFilter](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/web/filter/IdempotencyFilter.java) et son [test d’intégration](../../stock-infrastructure/src/test/java/com/aliCheikh/stock/infrastructure/web/filter/IdempotencyFilterIntegrationTest.java)
