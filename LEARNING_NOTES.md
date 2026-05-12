# LEARNING_NOTES.md — Stock Auto

Carnet de bord du projet de gestion de stock pour magasin de pièces auto.

**Auteur :** Ali Cheikh (M1 ALMA, Nantes Université)
**Triple objectif :** (1) monter en compétences pour le stage M2 et un CDI à la sortie, (2) construire un produit
vendable à un magasin client réel, (3) servir de portfolio.
**Stack actée :** Java 17 · Spring Boot 3.x · JPA/Hibernate · PostgreSQL · JWT · Flyway · Docker.

---

## Mode opératoire avec Claude (économiser les tokens)

Sur abonnement Pro à 20 $, chaque token compte. Discipline pour faire tenir le projet plusieurs mois sans dépasser les
limites :

- **Questions conceptuelles** ("pourquoi JPA cache le SQL", "qu'est-ce qu'un filter Spring Security") → claude.ai en
  navigateur, ou Gemini gratuit. Pas Cowork. Pas besoin que Claude touche aux fichiers pour expliquer un concept.
- **Questions code-spécifiques** (revue d'un mapper, debug d'un test, vérif d'alignement avec le domaine) → Cowork,
  c'est imbattable.
- **Une conversation par chantier**, pas une mega-conversation qui s'allonge.
- **Pour reprendre** : pointer ce fichier + le chantier en cours. Suffit pour remettre Claude à niveau en 30 secondes.
- **Questions précises** avec contexte concret, pas réflexions à voix haute.
- **Revues sur diff**, pas sur fichier entier.
- **Pilotage explicite des outils** : "réponds sans rien lire" ou "lis seulement X.java".

---

## Décisions de stack actées

| Décision            | Choix                                                             | Raison                                                                                                                                                                      |
|---------------------|-------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Framework web       | Spring Boot 3.x                                                   | Stack mainstream, doc énorme, demandée partout en entreprise                                                                                                                |
| Persistance         | JPA / Hibernate                                                   | Demandée en entreprise, simplifie le mapping. **Vigilance** : toujours activer `spring.jpa.show-sql=true` en dev pour ne pas devenir un junior qui livre du N+1 à l'aveugle |
| Base de données     | PostgreSQL via Docker                                             | Vrai SGBD, compétence vendable, types riches, support natif `UUID` et `JSONB`                                                                                               |
| Sécurité            | JWT access (15 min) + refresh (7 j en DB révocable)               | Atout CV ; impose la discipline sur la révocation, la rotation de clés, le stockage front (cookie httpOnly + SameSite=Strict, jamais localStorage)                          |
| Pagination          | Offset par défaut (`Pageable`) ; curseur sur `/stock-movements`   | Pour démontrer la maîtrise des deux approches en entretien                                                                                                                  |
| Pattern persistance | Persistence Model (entités JPA distinctes des agrégats)           | Le domaine reste 100 % POJO. Pas une seule annotation Spring/JPA dans `stock-domain/`                                                                                       |
| Build               | Maven multi-module avec `dependencyManagement` dans le parent POM | Versions centralisées, pas de divergence par module                                                                                                                         |

---

## Roadmap des chantiers (dans l'ordre)

### Chantier 0 — Corriger les bugs et dettes du backend review

**Pourquoi en premier :** les bugs de contrats `equals/hashCode` deviennent invisibles tant qu'on ne branche pas de Map
ou de Set, mais explosent silencieusement dès que l'infra JPA arrive (cache de premier niveau, dedup, comparaisons sur
identités reconstruites). À traiter avant tout, sinon on bâtit l'infra sur du sable.

**Référence :** `BACKEND_REVIEW.md` à la racine du projet.

**À traiter :**

- Critiques C1–C6 : `LocationId.hashCode()`, `MovementId.equals()`, `ShopId.hashCode()`, `AllocationResult` (record),
  null-checks dans les *Id, `Money.hashCode()` via `stripTrailingZeros`.
- Importants I1–I10 dans la checklist du review : signatures de repository (Optional), package `Dto`→`dto`,
  `SellProductCommand` multi-lignes (gros refactor, c'est l'évolution la plus structurante), `calculateGlobalStock` par
  `shopId`, frontière transactionnelle (note explicite, sera câblée par Spring), `InvalidMovementException` pour
  `createTransfer`, validation `ReceiveStockCommand`, double fetch dans `SellProductUseCase`, `dependencyManagement`
  dans le parent POM, ordonnancement `save → pullEvents`.

**Compétence acquise :** repérer les bugs de contrats Java invisibles, comprendre pourquoi DDD impose l'égalité par ID
sur les aggregate roots, distinguer exception métier vs exception technique.

**Décisions à prendre :** I5 frontière transactionnelle — option A (annotation `@Transactional` arrive avec Spring)
recommandée. I3 — accepter que ça change l'API : la nouvelle OpenAPI le prend déjà en compte.

**Notes au fil de l'eau :**
_(à remplir au fil des corrections)_

---

### Chantier 1 — Bootstrap Spring Boot 3 + PostgreSQL + profils

**Pourquoi :** sans Spring Boot, l'infra est un dossier vide. Ce chantier pose le socle qui va héberger tout le reste (
auto-config, IoC, profils, secrets).

**À apprendre :**

- Structure d'un projet Spring Boot multi-module (parent POM avec `spring-boot-dependencies` en BOM, le module infra
  avec les starters)
- Profils Spring (`dev`, `test`, `prod`) et `application-{profile}.yml`
- 12-factor config : aucun secret en clair dans le repo, tout via variables d'env
- `@SpringBootApplication`, `@EnableJpaRepositories`, et pourquoi limiter le scan
- Docker Compose pour Postgres en dev (volume persistant, healthcheck)

**Décisions à prendre :**

- Où pose-t-on `StockApplication` (classe `main`) ? Dans `stock-infrastructure` (pragmatique).
- BOM `spring-boot-dependencies` importé en `<dependencyManagement>` du parent ? Oui — pas de version Spring hardcodée
  par module.
- Utiliser `application.yml` ou `application.properties` ? YAML, plus lisible pour les profils.

**Pièges classiques :**

- Importer le starter parent `spring-boot-starter-parent` casse le multi-module ALMA. Préférer le BOM.
- Oublier de scope les scans Spring → tests ultra-lents et beans inattendus.
- Mettre les credentials Postgres dans `application.yml` versionné. Toujours via `${DB_PASSWORD}`.

**Notes au fil de l'eau :**
_(à remplir au fil de la mise en place)_

---

### Chantier 2 — Schéma DB et migrations Flyway V1

**Pourquoi :** la DB est le seul état persistant du système. Son schéma est versionné comme du code, jamais modifié à la
main en prod. Flyway est l'outil standard.

**À apprendre :**

- Modélisation relationnelle qui reflète le DDD sans le copier bêtement (un agrégat ≠ une table forcément, ex : `Sale`
  et `sale_line`)
- Contraintes d'intégrité : FK, CHECK (ex : `quantity > 0`), UNIQUE (ex : `product.reference`)
- Index utiles pour nos requêtes (paginations, filtres `belowThreshold`, journal par `productId`)
- Versioning : `V1__init.sql`, `V2__add_X.sql`, jamais modifier une migration déjà appliquée
- Pourquoi pas H2 en test (fidélité Postgres avec Testcontainers plus tard)

**Décisions à prendre :**

- UUID en clé primaire (cohérent avec le domaine) ou bigint auto-incrément (plus performant) ? **UUID**, on assume le
  coût pour la clarté avec le domaine.
- `BIGSERIAL` ou `IDENTITY` pour les colonnes auto ? `IDENTITY` (norme SQL) si on en utilise.
- Stocker `Money` comme deux colonnes `amount NUMERIC(15,2)` + `currency CHAR(3)` ? Oui, c'est le seul mapping correct
  pour préserver la précision.

**Pièges classiques :**

- Utiliser `FLOAT` ou `DOUBLE` pour les prix → arrondis. Toujours `NUMERIC`.
- Oublier les index sur les FK → joins lents.
- Modifier une migration déjà jouée → checksum mismatch, app refuse de démarrer.

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 3 — Persistance JPA + mappers + pagination offset/curseur

**Pourquoi :** c'est le chantier où on apprend le plus, et où on fait le plus de bugs invisibles si on ne sait pas ce
que JPA génère.

**À apprendre :**

- Pattern Persistence Model : entités JPA (`ProductJpaEntity`) distinctes des agrégats (`Product`), avec mappers dédiés
- Lifecycle JPA : transient / managed / detached / removed, et pourquoi `save()` n'est pas un `INSERT`
- N+1 : comment le détecter (logs Hibernate), comment le corriger (`@EntityGraph`, `JOIN FETCH`)
- Optimistic locking via `@Version` sur `StorageLocation` (deux ventes simultanées sur le même rayon)
- Pagination par offset (`Pageable` + `Page<T>`) et par curseur (keyset) — coder les deux
- `countQuery` : pourquoi le `count(*)` est parfois plus lent que la requête elle-même

**Décisions à prendre :**

- Activer `spring.jpa.open-in-view=false` ? **Oui systématiquement** — sinon les sessions Hibernate restent ouvertes
  pendant le rendu HTTP et masquent les N+1.
- `FetchType.LAZY` partout par défaut ? **Oui**, et on charge explicitement quand on en a besoin.
- Mapper manuel ou MapStruct ? **Manuel pour apprendre** (3 entités), MapStruct si on étend.

**Pièges classiques :**

- Coller `@Entity` directement sur l'agrégat domaine → c'est le piège du tutoriel typique. Ne pas le faire.
- `@OneToMany` avec `FetchType.EAGER` → N+1 à coup sûr.
- `equals/hashCode` basés sur l'ID JPA généré → deux entités non persistées sont "égales" (id null).

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 4 — Controllers REST + DTOs alignés sur OpenAPI

**Pourquoi :** c'est l'interface du backend avec le monde. Les DTOs HTTP sont distincts des Commands applicatifs (qui
sont distincts des agrégats domaine). Trois couches de types — apprendre pourquoi.

**À apprendre :**

- Controllers fins : parser DTO HTTP → construire Command → appeler use case → mapper réponse. Pas de logique métier.
- Bean Validation (`@Valid`, `@NotBlank`, `@Positive`)
- Sérialisation Jackson des records et de `Money`
- 201 vs 202 vs 200 (sales = synchrone 201, receipts/transfers = asynchrone 202 selon l'OpenAPI)
- `ResponseEntity<T>` vs `@ResponseStatus`

**Décisions à prendre :**

- DTOs en records ou classes ? **Records**, immuables et concis.
- Validation des DTOs au niveau controller (Bean Validation) ou redéplacée dans la commande applicative ? **Les deux** :
  Bean Validation côté HTTP pour les 400 propres, validation domaine dans les commands pour la défense en profondeur.

**Pièges classiques :**

- Un controller qui accède directement au repository → casse l'archi hexagonale.
- Renvoyer une entité JPA en JSON → fuite du modèle de persistance, et risque de chargement lazy en plein rendu HTTP.
- Sérialiser des dates sans timezone explicite.

**Notes au fil de l'eau :**

**Ticket 4.1 — GET /health (terminé)**

- Premier endpoint Spring MVC concret. Mécanique DispatcherServlet comprise et défendable.
- Décisions design : préfixe `/api/v1` au niveau classe (pas context-path), enum Java pour
  `status`, `Instant` pour `timestamp`.
- Slice test `@WebMvcTest` + MockMvc + JSONPath.
- Piège retenu : `@RestController` oublié → 404 silencieux, pas d'erreur démarrage.

**Ticket 4.2 — GET /products/{productId} (terminé)**

- Premier endpoint de lecture avec mapping domaine → DTO. CQRS pragmatique : le controller
  dépend directement du port `ProductRepository`, pas de use case pour les lectures simples.
- Concepts ancrés : `@PathVariable`, `ResponseEntity<T>`, `Optional` + pattern fonctionnel
  `.map().orElseGet()`, utility class pattern (`final` + constructeur privé), flag
  `-parameters` du compilateur Maven, distinction bean Spring vs annotation de configuration.
- Migration `@MockBean` → `@MockitoBean` (déprécié depuis Spring Boot 3.4).
- Piège retenu : `category` au lieu de `categoryId` dans le DTO — divergence silencieuse au
  contrat OpenAPI. Argument fort en faveur des tests `jsonPath` stricts.

**Ticket 4.3 — GET /products paginé (terminé)**

- Premier endpoint de liste, pose la convention de pagination pour tous les futurs endpoints
  de collection.
- Approche **TDD complète** : 6 cycles Red-Green-Refactor, un test par scénario d'acceptation,
  commits atomiques par cycle.
- Décisions design : port domaine en `page/size` (cohérent REST + adapter trivial), wrapper
  DTOs custom (`PageOfProductResponse`, `PageMetaResponse`) plutôt que `Page<T>` de Spring
  Data — contrat propre indépendant du framework.
- Mapping `Product → ProductResponse` réutilisé via method reference pour éviter la
  duplication (DRY).
- Validation des query params via `@Validated` au niveau classe + `@Min/@Max` sur les
  paramètres + `@ExceptionHandler` local pour mapper en 400.
- Pièges retenus :
  - `ConstraintViolationException` retournée en 500 par défaut (pas 400 comme `@Valid` sur
    `@RequestBody`). Handler explicite requis.
  - Calcul de `totalPages` doit utiliser `totalElements`, pas `content.size()`.
  - `count()` retourne `long`, alignement de type dans tous les DTOs/mappers.

---

### Chantier 5 — Gestion globale des erreurs (RFC 7807)

**Pourquoi :** une exception métier qui sort en 500 Internal Server Error est un bug. Chaque exception domaine doit être
mappée vers un `ProblemDetail` propre avec le bon code HTTP et un code machine pour le front.

**À apprendre :**

- `@RestControllerAdvice` et `@ExceptionHandler`
- Standard RFC 7807 (`application/problem+json`)
- Mapping exception métier → code HTTP : `STOCK_INSUFFICIENT` → 409, `PRODUCT_NOT_FOUND` → 404, `TRANSFER_CROSS_SHOP` →
  422, `VALIDATION_FAILED` → 400, `INVALID_MOVEMENT` → 422
- Différence exception métier (cas d'usage prévu, mappable) vs technique (bug, 500)
- Propagation du `traceId` dans le ProblemDetail pour corrélation côté logs

**Décisions à prendre :**

- Créer un `ErrorCode` enum côté infra qui mappe chaque exception domaine → code machine ? **Oui**, c'est ce qui permet
  au front de réagir typé.

**Pièges classiques :**

- Un handler qui catch `Exception` en dernier rideau et renvoie 500 sans trace → on perd l'information.
- Logger la stack complète pour les 4xx → spam des logs.

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 6 — Sécurité Spring Security + JWT (access + refresh)

**Pourquoi :** un logiciel sans authentification n'est pas vendable. JWT est l'occasion d'apprendre proprement le
pattern access + refresh, qui est la question piège n° 1 en entretien junior.

**À apprendre :**

- Filter chain Spring Security (qui appelle qui dans quel ordre)
- Génération + validation JWT (HMAC ou RSA — RSA si on veut maquetter la rotation de clés)
- Endpoint `POST /auth/login` (email/password → access + refresh)
- Endpoint `POST /auth/refresh` (refresh token → nouveau access ; refresh révocable en DB)
- Endpoint `POST /auth/logout` (révocation du refresh)
- BCrypt pour les passwords (`BCryptPasswordEncoder`)
- Autorisation par rôle (`@PreAuthorize("hasRole('OWNER')")`)
- Pourquoi pas de CSRF en stateless (si tokens non-cookie) — ou inversement, pourquoi en garder en cookie httpOnly

**Décisions à prendre :**

- Stocker JWT côté front en cookie httpOnly ou en mémoire JS ? **Cookie httpOnly + SameSite=Strict** — protégé du XSS,
  exposé au CSRF qu'on couvre via SameSite.
- HMAC (un seul secret) ou RSA (clé privée signature, publique vérif) ? **HMAC pour démarrer** (1 service), RSA si on
  splitte un jour.
- Durée de vie : access 15 min, refresh 7 jours.

**Pièges classiques :**

- Stocker les JWT côté front dans `localStorage` → XSS fatal. Toujours httpOnly.
- Signer avec un secret de dev (`"changeme"`) commité dans le repo.
- Oublier de rotater la clé de signature.

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 7 — Idempotency-Key pour les mutations

**Pourquoi :** quand le réseau coupe au milieu d'un POST `/sales`, le client retente. Sans idempotency, la vente est
créée deux fois. C'est un standard pour toute API moderne (Stripe, etc.) — démontre la maturité du dev.

**À apprendre :**

- Filter Spring qui intercepte les POST sur `/sales`, `/stock-receipts`, `/stock-transfers`
- Table `idempotency_key (key, request_hash, response_status, response_body, created_at)` — TTL 24h
- Comportement : même clé + même hash de body → on rejoue la réponse. Même clé + body différent → 422.
- INSERT ... ON CONFLICT pour gérer la concurrence sans race.

**Décisions à prendre :**

- TTL : 24 h (cohérent avec l'OpenAPI).
- Stocker le response body en JSONB Postgres ou en TEXT ? **JSONB**, requêtable et compact.

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 8 — Publication d'événements domaine

**Pourquoi :** les events (`LowStockAlert`, `SaleCompleted`, `ShopFloorLow`, etc.) sont déjà émis par les agrégats.
Reste à les publier vers un bus pour qu'un listener puisse réagir (envoi mail, mise à jour read-model, notification UI).

**À apprendre :**

- `ApplicationEventPublisher` de Spring (synchrone, in-process, transactionnel)
- `@TransactionalEventListener(phase = AFTER_COMMIT)` — pourquoi ne jamais publier pendant la transaction
- Pattern outbox (juste à mentionner) si on passe à du messaging externe plus tard

**Décisions à prendre :**

- Synchrone in-process pour la V1 (suffit pour un magasin) ; Kafka/RabbitMQ pour la V2 si multi-shops.

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 9 — Observabilité (Actuator + logs structurés + traceId)

**Pourquoi :** un logiciel en prod sans logs structurés et sans health endpoint, c'est un logiciel qu'on ne peut pas
exploiter. Compétence ops minimale obligatoire.

**À apprendre :**

- Spring Boot Actuator (`/actuator/health`, `/actuator/info`, `/actuator/metrics`)
- Logback en JSON (lib `logstash-logback-encoder`)
- MDC pour propager le `traceId` à travers la requête
- Quoi ne JAMAIS logger : passwords, tokens, données personnelles brutes

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 10 — Tests d'intégration avec Testcontainers

**Pourquoi :** les tests domaine sont déjà solides. Ce qui manque : prouver que tout le pipeline (HTTP → controller →
use case → repository → Postgres) fonctionne. Testcontainers lance une vraie Postgres jetable en Docker pendant le test.

**À apprendre :**

- Pyramide de tests : unit (rapide) > integration (moyen) > e2e (lent)
- Pourquoi Testcontainers plutôt que H2 (fidélité)
- Stratégie d'isolation : transaction rollback ou schéma par test
- `@SpringBootTest` + `@Testcontainers` + `@DynamicPropertySource`

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 11 — Déploiement Docker Compose pour le magasin

**Pourquoi :** le magasin client doit pouvoir lancer le logiciel sur sa machine en une commande. C'est la dernière mile
qui transforme un projet d'école en produit utilisable.

**À apprendre :**

- Dockerfile multi-stage (build → runtime)
- `docker-compose.prod.yml` avec Postgres + app + volume + healthcheck + restart policy
- Script de backup quotidien (`pg_dump` cron)
- Variables d'env injectées via `.env` non versionné

**Notes au fil de l'eau :**
_(à remplir)_

---

### Chantier 12 — Reprise du frontend Angular

**Pré-requis :** chantiers 0 à 11 terminés, backend déployable, OpenAPI gelée.

Génération des fichiers HTML/SCSS pour les 16 écrans validés en preview. Référence des maquettes : voir l'historique des
conversations Cowork de fin avril 2026 (sélecteur produit universel, écrans Vente / Réception / Transfert / Catalogue /
Mouvements / Alertes / Admin). Strictement aligné sur le contrat OpenAPI livré par l'infra.

**Notes au fil de l'eau :**
_(à remplir)_

---

## Compétences à acquérir / consolider — calendrier d'application

Liste des compétences à intégrer au projet, classées par urgence et accrochées explicitement à un chantier ou une phase.

**Règle d'or : chaque compétence doit être exercée en contexte, jamais étudiée à part.** Lire un bouquin Spring Security
dans le métro sans avoir de code à protéger, c'est du temps perdu. Câbler `JwtAuthenticationFilter` au chantier 6 sur le
vrai backend, c'est un apprentissage qui reste. Ce calendrier n'est pas un programme de cours, c'est une feuille de route
pour brancher chaque compétence à un moment où elle sert pour de vrai.

### 🔴 Urgentes — sur le projet stock

**1. Jira + Agile/Scrum**
À mettre en place **dès maintenant (chantier 0)** et à utiliser pour TOUS les chantiers. Découper chaque chantier en
sprints d'une à deux semaines, chaque tâche du `BACKEND_REVIEW.md` et de la roadmap devient un ticket Jira avec critères
d'acceptation. C'est ce qui permet de répondre en entretien : "j'ai mené ce projet en sprints de deux semaines, voici mon
board avec les épics par chantier." Sans Jira, la même réalité passe pour de l'amateurisme. Coût de mise en place
(gratuit, 30 minutes) dérisoire face au bénéfice CV.

**2. Spring Security + JWT**
À appliquer pleinement au **chantier 6**. Mais commencer à lire la doc Spring Security dès le **chantier 4** (controllers
REST), parce que la filter chain et le mécanisme d'authentification doivent être compris *avant* d'avoir des endpoints à
protéger.

**3. Tests d'intégration**
Le **chantier 10** y est dédié, mais ne pas attendre. Dès le **chantier 3 (JPA)**, écrire un premier test d'intégration
avec Testcontainers sur un repository — c'est le moment où le code touche pour la première fois à une vraie DB, c'est le
moment de tester pour de vrai.

**4. Git avancé**
Cross-cutting, **dès maintenant**. Utiliser systématiquement : branches feature par chantier, commits atomiques avec
messages conformes (Conventional Commits), rebase interactif pour nettoyer avant merge, tags annotés pour les fins de
chantier. Apprendre `git bisect` la première fois qu'on chasse un bug introduit dans la semaine — c'est le bon moment.

### 🟠 Importantes — fil rouge des chantiers backend

**5. Docker**
Premier contact au **chantier 1** (Postgres en Docker Compose pour le dev). Maîtrise approfondie au **chantier 11**
(multi-stage Dockerfile pour l'app, compose prod, healthchecks, volumes). Ne pas attendre le 11 pour comprendre les
concepts — il faut être à l'aise avec image vs container, layer caching, ports, volumes, dès le 1.

**6. GitHub Actions + SonarQube**
À installer **dès le chantier 1** sur une CI minimale (build + tests Maven). Étendre progressivement : ajouter SonarCloud
(gratuit) au **chantier 3** quand le code commence à croître, ajouter un job Docker au **chantier 11**. Pas un chantier
dédié — un fil rouge qui s'étoffe au fil des chantiers.

**7. SQL avancé**
Exercé dès le **chantier 2** (modélisation, contraintes, index) et surtout au **chantier 3** (analyse des requêtes
générées par Hibernate, `EXPLAIN ANALYZE` pour traquer les N+1, index sur les colonnes filtrées). C'est l'occasion idéale
parce que JPA cache le SQL — mais il faut le voir, le lire, le comprendre.

**8. API Monitoring (Spring Actuator)**
Chantier dédié : **chantier 9**. Mais brancher `/actuator/health` *minimalement* dès le **chantier 1** : c'est trivial à
activer et ça donne tout de suite un endpoint pour vérifier que l'app tourne en local sans appeler les vrais endpoints.

### 🟡 Utiles — fin de projet et préparation entretiens

**9. Architecture défendable à l'oral**
À exercer en continu, mais à *travailler explicitement* à partir du **chantier 5-6**, quand il y aura assez de matière à
présenter. Concrètement : à la fin de chaque chantier, écrire un paragraphe qui répond à "qu'est-ce que j'ai construit,
pourquoi ces choix, quel autre choix aurais-je pu faire ?". C'est la préparation aux entretiens et soutenances, déposée
semaine après semaine plutôt qu'écrite en panique la veille.

**10. Notions cloud**
Quand le **chantier 11** sera fini (app conteneurisée, déployable), envisager un déploiement test sur AWS ou GCP — pas
pour le faire tourner réellement chez le client, mais pour comprendre les concepts (instance, security group, RDS,
secrets manager). Ça transforme "j'ai dockerisé" en "je comprends comment on déploie pour de vrai".

**11. Microservices — comprendre le concept et quand l'appliquer**
Étude *concept et trade-offs*, pas pratique sur ce projet (le projet est volontairement monolithique modulaire, c'est le
bon choix pour son scope). Lire Sam Newman, comprendre les patterns (saga, event sourcing, service mesh), et savoir
répondre en entretien à "pourquoi vous n'avez pas fait du microservice ?" — la bonne réponse n'est jamais "parce que je
savais pas faire", c'est toujours "parce que pour un magasin, le coût opérationnel d'un déploiement microservices ne se
justifie pas, et qu'avec mon archi hexagonale je peux extraire un service plus tard si le besoin se présente."

### 🟢 Différenciantes — après la livraison V1 et projets parallèles

**12. NoSQL / MongoDB**
Hors projet stock. Soit comme exercice indépendant, soit — **bien plus parlant en entretien** — comme deuxième
implémentation du port `ProductRepository` pour démontrer concrètement l'intérêt de l'archi hexagonale : un seul port,
deux adaptateurs (`JpaProductRepository` et `MongoProductRepository`), zéro changement dans le métier. Cette démo seule
peut faire la différence sur un poste senior.

**13. React.js**
Hors projet (le front est en Angular). Soit comme deuxième front sur un endpoint isolé du back stock (dashboard de
stats), soit projet séparé. Utile pour les offres qui exigent React.

**14. Communication technique**
Pas de chantier dédié, mais à exercer chaque fois qu'on écrit un README, un commentaire de PR, une explication dans
`LEARNING_NOTES.md`. Travailler la concision : une page lue par un dev senior en 90 secondes vaut mieux qu'un essai de
cinq pages que personne ne lira.

**15. Lecture de code legacy**
Hors projet stock (le nôtre est neuf). Exercice à faire en parallèle : lire le code source d'un projet open-source connu
(Spring Boot lui-même est un excellent terrain : choisir un module, comprendre la filter chain, retracer un bean). Cette
compétence est la plus sous-estimée et la plus demandée en CDI.

**16. .NET / C# (pour Eurofins IT)**
Hors projet, ciblé entreprise. À démarrer si on vise spécifiquement Eurofins (ou autre boîte .NET). Apprendre les
fondamentaux (LINQ, async/await, ASP.NET Core) suffit pour la plupart des entretiens — pas besoin de redéployer un projet
complet en C#. Faire le mapping mental : la quasi-totalité de ce qu'on apprend en Spring se transpose en ASP.NET Core
(DI, MVC, Entity Framework ≈ JPA).

### Synthèse — à quel chantier ouvrir quoi

| Chantier              | Compétences à activer / consolider                                                  |
|-----------------------|-------------------------------------------------------------------------------------|
| 0 (en cours)          | 1. Jira · 4. Git avancé                                                             |
| 1 (bootstrap Spring)  | 5. Docker · 6. GitHub Actions · 8. Actuator (minimal)                               |
| 2 (DB + Flyway)       | 7. SQL avancé (modélisation)                                                        |
| 3 (JPA)               | 3. Tests d'intégration (Testcontainers) · 7. SQL avancé (perf) · 6. SonarCloud      |
| 4 (controllers REST)  | 2. Spring Security (lecture doc)                                                    |
| 5–6 (erreurs + sécu)  | 2. Spring Security + JWT (plein) · 9. Archi défendable (début)                      |
| 9 (observabilité)     | 8. Actuator (plein)                                                                 |
| 10 (tests intégration)| 3. Tests d'intégration (plein)                                                      |
| 11 (Docker prod)      | 5. Docker (plein) · 10. Cloud · 6. CI Docker                                        |
| après projet          | 11. Microservices · 12. NoSQL · 13. React · 15. Legacy · 16. .NET                   |
| en continu            | 4. Git · 14. Communication technique · 9. Archi défendable                          |

**Notes au fil de l'eau :**
_(à remplir au fil de l'acquisition)_

---

## Concepts maîtrisés (à compléter au fil du projet)

_Liste qu'on remplit ensemble à chaque concept réellement compris (pas juste lu). Sert de matériel pour les entretiens :
tu pourras parler concret de chaque ligne._

- [ ] DDD : agrégats, invariants, factory methods, events, value objects
- [ ] Architecture hexagonale : ports/adapters, sens des dépendances
- [ ] Spring Boot : auto-config, profils, IoC, BOM
- [ ] JPA : Persistence Model, lifecycle, N+1, optimistic locking
- [ ] Pagination offset vs curseur
- [ ] RFC 7807 problem+json
- [ ] JWT access + refresh, révocation, stockage front sécurisé
- [ ] Idempotency-Key pattern
- [ ] Domain events synchrones in-process avec `@TransactionalEventListener`
- [ ] Testcontainers
- [ ] Docker multi-stage + Compose

---

## Glossaire métier (extrait du langage ubiquitaire)

| Terme                   | Sens                                                                                 |
|-------------------------|--------------------------------------------------------------------------------------|
| Produit / Référence     | `Product` agrégat, `reference` est la SKU lisible                                    |
| Magasin (SHOP_FLOOR)    | Le rayon où le client voit la pièce                                                  |
| Réserve (BACKSTOCK)     | L'arrière-boutique                                                                   |
| Shop                    | La boutique physique (peut contenir 1 SHOP_FLOOR + 1 BACKSTOCK)                      |
| Stock global            | Somme des stocks de toutes les locations d'un shop pour un produit                   |
| Seuil global            | `Product.minimumGlobalThreshold` — sous ce niveau, alerte de réappro                 |
| Indicateur de stock bas | `StorageLocation.lowStockIndicator` — propre à un emplacement (typiquement le rayon) |
| Réception               | Entrée de marchandise depuis un fournisseur (ENTRY)                                  |
| Vente                   | Sortie de marchandise vers un client (EXIT)                                          |
| Transfert               | Mouvement interne entre 2 emplacements du même shop (TRANSFER)                       |
| Mouvement               | Trace immuable d'une opération sur le stock                                          |
| LowStockAlert           | Event émis quand le stock global passe sous le seuil                                 |
| ShopFloorLow            | Event émis quand un emplacement SHOP_FLOOR passe sous son indicateur                 |
