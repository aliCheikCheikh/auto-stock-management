# 0002 — DDD tactique et architecture hexagonale, choisis aussi pour les pratiquer

**Statut :** appliquée

## Contexte

Le domaine est de taille modeste : un magasin, des produits, deux emplacements, des ventes et des versements. Le DDD est généralement recommandé pour des domaines complexes, et une architecture en couches classique (contrôleur → service → repository Spring Data) aurait suffi pour livrer.

Ce projet avait aussi un objectif personnel : pratiquer sur un vrai besoin les notions étudiées en master (agrégats, objets-valeurs, ports et adaptateurs), plutôt que sur un exercice.

## Décision

Appliquer volontairement :

- le **DDD tactique** : agrégats (`Sale`, `Product`, `StorageLocation`), objets-valeurs (`Money`, `PhoneNumber`, identifiants typés), services de domaine (`StockAllocationService`), événements (`SaleCompleted`, `StockReceived`), langage métier documenté ;
- l’**architecture hexagonale** : les cas d’usage ne connaissent que des ports (`SaleRepository`, `TransactionRunner`, `EventPublisher`…), implémentés par des adaptateurs JPA et Spring.

Le DDD **stratégique** (plusieurs contextes délimités, cartographie) n’est pas appliqué : le domaine n’en justifie pas.

## Conséquences

- Les règles importantes sont écrites une seule fois, dans le domaine : refus d’un versement supérieur au reste dû, vente à crédit sans client, stock insuffisant.
- Ces règles se testent en Java pur : 100 tests domaine et 126 tests application s’exécutent sans Spring ni base.
- Coût assumé : plus de classes (entités JPA distinctes des objets métier, mappers, adaptateurs) que dans une application CRUD. Sur un projet plus simple en équipe, je choisirais d’abord une architecture en couches et n’introduirais ces séparations qu’à l’apparition de règles métier réelles.
- La présence de tests ne suffit pas à prouver une démarche TDD ; ce dépôt ne le revendique pas.

## Dans le code

- Domaine : [Sale](../../stock-domain/src/main/java/com/aliCheikh/stock/domain/model/sale/Sale.java), [Money](../../stock-domain/src/main/java/com/aliCheikh/stock/domain/model/shared/Money.java), [StockAllocationService](../../stock-domain/src/main/java/com/aliCheikh/stock/domain/service/StockAllocationService.java)
- Port et adaptateur : [SaleRepository](../../stock-domain/src/main/java/com/aliCheikh/stock/domain/model/sale/port/SaleRepository.java) → [SaleJpaRepositoryAdapter](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/persistence/adapter/SaleJpaRepositoryAdapter.java)
- Langage métier et agrégats : [documentation Antora](../../antora-docs/modules/domain/pages)
