# 0001 — Un seul déployable, trois modules Maven

**Statut :** appliquée

## Contexte

Le magasin a un seul site, quelques utilisateurs et un seul serveur. Une vente modifie en même temps la vente, les quantités par emplacement et le journal des mouvements ; un remboursement modifie la vente et le solde du client. Ces écritures doivent réussir ou échouer ensemble.

## Décision

Livrer un seul JAR Spring Boot, découpé en trois modules Maven :

- `stock-domain` : modèle métier, sans dépendance de production à un framework ;
- `stock-application` : cas d’usage et ports ;
- `stock-infrastructure` : REST, sécurité, JPA, Flyway, configuration Spring.

Les dépendances vont dans un seul sens : `infrastructure → application → domain`.

## Conséquences

- Une transaction PostgreSQL suffit pour garder les écritures cohérentes ; pas de transactions distribuées ni de broker.
- Le découpage est vérifié à la compilation : le domaine ne peut pas importer Spring par accident.
- Tout est déployé ensemble. Pour un magasin, c’est un avantage (un seul service à surveiller) ; pour plusieurs équipes indépendantes, ce serait une limite.

## Dans le code

- [pom.xml](../../pom.xml) (modules), [stock-domain/pom.xml](../../stock-domain/pom.xml) (dépendances de test uniquement)
- [UseCaseConfiguration](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/config/UseCaseConfiguration.java) : assemblage des cas d’usage
