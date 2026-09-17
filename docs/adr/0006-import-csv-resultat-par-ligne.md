# 0006 — Import CSV en deux temps, résultat enregistré ligne par ligne

**Statut :** appliquée

## Contexte

Les arrivages se préparent souvent dans un tableur. Une erreur de catégorie ou de prix ne doit pas être découverte après l’écriture en base, et un fichier renvoyé par erreur ne doit pas doubler le stock.

## Décision

1. **Aperçu** : le fichier est analysé sans rien écrire ; chaque ligne indique l’action prévue (créer le produit ou compléter le stock) et ses erreurs.
2. **Exécution** : l’utilisateur confirme les lignes choisies. Chaque ligne est traitée dans sa propre transaction et son résultat est enregistré dans un journal associé à l’identifiant d’import.

Limites : 500 lignes et 1 Mo par fichier.

## Conséquences

- Une ligne en échec n’annule pas les lignes déjà importées ; le rapport indique précisément ce qui a été fait.
- Relancer la même exécution retrouve les lignes déjà traitées au lieu de les rejouer.
- Il n’y a pas de transaction globale « tout ou rien » sur le fichier : c’est un choix, pas un oubli.

## Dans le code

- [PrepareStockReceiptImportUseCase](../../stock-application/src/main/java/com/aliCheikh/stock/application/usecase/PrepareStockReceiptImportUseCase.java), [ExecuteStockReceiptImportUseCase](../../stock-application/src/main/java/com/aliCheikh/stock/application/usecase/ExecuteStockReceiptImportUseCase.java)
- Journal : [V17__add_stock_receipt_import_ledger.sql](../../stock-infrastructure/src/main/resources/db/migration/V17__add_stock_receipt_import_ledger.sql)
- [StockReceiptImportEndToEndTest](../../stock-infrastructure/src/test/java/com/aliCheikh/stock/infrastructure/StockReceiptImportEndToEndTest.java)
