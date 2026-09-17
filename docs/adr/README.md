# Décisions d’architecture (ADR)

Chaque fiche décrit un choix structurant, le contexte qui l’a motivé et ce qu’il coûte. Les fiches ont été rédigées en septembre 2026 à partir du code existant : elles documentent des décisions déjà appliquées.

| N° | Décision | Statut |
| --- | --- | --- |
| [0001](0001-monolithe-modulaire.md) | Un seul déployable, trois modules Maven | Appliquée |
| [0002](0002-ddd-hexagonal-par-choix-d-apprentissage.md) | DDD tactique et architecture hexagonale, choisis aussi pour les pratiquer | Appliquée |
| [0003](0003-solde-calcule-depuis-les-versements.md) | Le reste dû est calculé à partir du journal des versements | Appliquée |
| [0004](0004-concurrence-et-idempotence.md) | Verrou optimiste sur le stock, pessimiste sur les paiements, clé d’idempotence | Appliquée |
| [0005](0005-session-par-cookies-httponly.md) | Session JWT dans des cookies HttpOnly | Appliquée |
| [0006](0006-import-csv-resultat-par-ligne.md) | Import CSV en deux temps, résultat enregistré ligne par ligne | Appliquée |

Modèle : contexte → décision → conséquences → où le voir dans le code.
