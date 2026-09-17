# 0003 — Le reste dû est calculé à partir du journal des versements

**Statut :** appliquée

## Contexte

Un client peut payer une partie d’une vente puis revenir plusieurs fois. Stocker à la fois « montant payé » et « reste dû » crée un risque : deux valeurs qui ne concordent plus après une correction ou une erreur.

## Décision

L’agrégat `Sale` conserve la liste des versements (`Payment`). Le montant encaissé est la somme de cette liste ; le reste dû est `total − encaissé`. Aucun solde n’est stocké séparément.

Les montants utilisent l’objet-valeur `Money` (`BigDecimal` + devise), qui refuse les opérations entre devises différentes. Le prix unitaire est copié dans la ligne de vente, pour qu’un changement de catalogue ne modifie pas les ventes passées.

## Conséquences

- L’historique des versements explique toujours le solde affiché.
- `recordPayment` refuse un versement si la vente est déjà soldée ou si le montant dépasse le reste dû.
- Les écrans de liste (créances, tableau de bord) utilisent des requêtes dédiées pour éviter de recharger chaque agrégat.

## Dans le code

- [Sale.recordPayment](../../stock-domain/src/main/java/com/aliCheikh/stock/domain/model/sale/Sale.java)
- [SalePaymentTest](../../stock-domain/src/test/java/com/aliCheikh/stock/domain/model/sale/SalePaymentTest.java)
- Migration du journal : [V13__add_payment_ledger.sql](../../stock-infrastructure/src/main/resources/db/migration/V13__add_payment_ledger.sql)
