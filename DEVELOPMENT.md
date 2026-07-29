# Démarrage local du backend

Le backend ne choisit volontairement aucun profil Spring par défaut. En production, une
configuration incomplète doit interrompre le démarrage plutôt que charger les identifiants et le
compte d'amorçage du profil `dev`.

## Prérequis

- Java 17 ;
- Maven ;
- Docker avec le moteur démarré.

## Démarrer PostgreSQL

Depuis la racine du dépôt :

```bash
docker compose up -d postgres
docker compose ps
```

Le service `auto-stock-postgres` doit être indiqué comme `healthy` avant de lancer l'application.

## Démarrer depuis IntelliJ IDEA

Sélectionner la configuration partagée **Backend local — dev**, puis cliquer sur Run. Elle active
explicitement `SPRING_PROFILES_ACTIVE=dev` et utilise le module `stock-infrastructure`.

La configuration temporaire générée automatiquement à partir de `StockApplication.main()` ne
contient pas ce profil et ne doit donc pas être utilisée pour le développement local.

## Démarrer depuis un terminal

```bash
mvn -pl stock-infrastructure spring-boot:run -Dspring-boot.run.profiles=dev
```

Un démarrage réussi contient notamment :

```text
The following 1 profile is active: "dev"
HikariPool-1 - Start completed.
Successfully validated 15 migrations
Tomcat started on port 8080
```

## Diagnostic rapide

- `Failed to configure a DataSource: 'url' attribute is not specified` : le profil `dev` n'est pas
  actif ;
- `Connection refused` sur le port `5432` : PostgreSQL n'est pas démarré ou n'est pas encore
  `healthy` ;
- erreur Flyway : ne jamais modifier une migration déjà appliquée ; comparer la version et le
  checksum signalés avant toute intervention.
