# REVISIONS.md — Stock Auto

Carnet de révision quotidien. Tout ce qui a été appris en mentoring, classé par chantier
et par concept. Objectif : être en béton sur Spring Boot, Angular, Docker, PostgreSQL.

Méthode de révision recommandée : 10 minutes par matin, on lit une section, on **reformule
à voix haute** les concepts avec ses propres mots. Si on bute, on revient ici. Si on a la
formulation fluide, on coche mentalement et on passe au suivant.

---

## Chantier 4 — API REST, DTOs, validation, OpenAPI

### Ticket 4.1 — `GET /api/v1/health` (terminé)

#### 1. Le cycle complet d'une requête HTTP dans Spring

Quand un client envoie `GET /api/v1/health` au serveur :

1. **Tomcat** (serveur web embarqué par Spring Boot) écoute sur le port 8080 et reçoit la
   requête HTTP brute.
2. Tomcat la passe au **DispatcherServlet** — le chef d'orchestre central de Spring MVC.
3. Le DispatcherServlet consulte sa **table de routage** (HandlerMapping). Cette table a
   été construite au démarrage de l'app en scannant les classes annotées `@RestController`
   et leurs méthodes annotées `@GetMapping`, `@PostMapping`, etc.
4. Il trouve la méthode Java qui matche **exactement** le couple `(méthode HTTP, chemin)`
   — pas de matching approximatif. Si rien ne matche, 404. Si plusieurs matchent, l'app
   refuse de démarrer.
5. Il invoque cette méthode, récupère l'objet retourné.
6. **Jackson** sérialise cet objet en JSON, en utilisant la **réflexion Java** pour
   inspecter ses accesseurs publics.
7. Spring renvoie le JSON dans le body HTTP avec le bon status code et le bon
   Content-Type.

Image mentale à garder : Spring MVC = un standard téléphonique qui consulte un annuaire
`(verbe HTTP + URL) → méthode Java`, plus une étape automatique de conversion Java ↔ JSON.

#### 2. `@RestController` et le piège du 404 silencieux

`@RestController` est l'annotation qui dit à Spring : *"cette classe est un controller,
scanne-la au démarrage, et sérialise les valeurs de retour de ses méthodes en JSON dans
le body de la réponse"*. C'est l'équivalent de `@Controller` + `@ResponseBody` implicite.

**Piège classique à connaître** : si on oublie `@RestController`, l'application démarre
**en silence**, sans erreur ni warning. Mais la classe n'est pas détectée comme controller,
ses routes ne sont pas enregistrées, et tout appel à ces routes retourne **404 Not Found**.

Distinction à intégrer : Spring distingue **configuration erronée** (plante au démarrage)
et **configuration absente** (marche en silence). Oublier `@RestController` tombe dans la
deuxième catégorie. C'est invisible jusqu'à ce qu'on tente l'endpoint et reçoive 404 sans
comprendre.

Réflexe entreprise : à chaque nouveau controller, on teste immédiatement avec `curl` ou
un test `@WebMvcTest`. On n'attend pas.

#### 3. Mapping des routes

`@RequestMapping("/api/v1/health")` au niveau **classe** préfixe toutes les routes des
méthodes. `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
au niveau **méthode** définissent la route spécifique.

Le matching est **exact** : `(verbe HTTP, chemin)`. Pas d'approximation. Si deux méthodes
gagnent simultanément, Spring lève `IllegalStateException: Ambiguous mapping` au
démarrage — la config erronée est détectée tôt, c'est sain.

#### 4. Versioning de l'API — pourquoi `/api/v1`

Préfixer les routes avec `/api/v1` permet de faire évoluer le contrat de l'API sans
casser les clients existants. Quand un changement breaking devient nécessaire
(renommage de champ, restructuration), on publie une `v2` en parallèle. Les anciens
clients restent sur `v1`, les nouveaux développements migrent vers `v2`. On annonce
une **sunset date** pour `v1`, et on supprime quand tous les clients ont migré.

**Décision de design actée** : on préfixe au niveau **classe**
(`@RequestMapping("/api/v1/...")`) plutôt qu'au niveau serveur
(`server.servlet.context-path`). Raison : le versioning est une décision applicative,
pas serveur. Avec context-path, on serait coincé pour faire coexister v1 et v2 (toute
l'app vivrait derrière un seul préfixe).

#### 5. Records Java pour les DTOs

Un record est un type Java déclaré en une seule ligne :

```java
public record HealthStatusResponse(HealthStatusValue status, String version, Instant timestamp) { }
```

Le compilateur génère automatiquement : champs privés `final`, constructeur canonique,
accesseurs publics (sans préfixe `get`, juste le nom du champ : `status()`), `equals`,
`hashCode`, `toString`. C'est **concis** et **immutable** par design.

Pour un DTO de **réponse** (construit côté serveur), pas besoin de validation interne.
Pour un DTO de **requête** ou un Command applicatif, on peut ajouter un **compact
constructor** :

```java
public record SomeCommand(String field) {
    public SomeCommand {                                       // pas de parenthèses
        Objects.requireNonNull(field, "field cannot be null");
        if (field.isBlank()) throw new IllegalArgumentException("blank");
        // pas besoin d'assigner this.field = field, c'est automatique
    }
}
```

#### 6. Sérialisation Jackson — comment ça marche concrètement

Jackson **n'a aucune configuration** à recevoir pour un record simple. Il utilise la
**réflexion Java** au runtime : il demande à la JVM la liste des méthodes publiques de
la classe, il filtre les accesseurs (pour un record : les méthodes `nomDuChamp()`), il
invoque chaque accesseur dynamiquement, et il construit le JSON avec le nom du champ
comme clé.

Si on veut un nom de clé JSON différent du nom du champ Java, on utilise l'annotation
`@JsonProperty("autre_nom")` sur le composant du record.

#### 7. Types Java pour la sérialisation JSON

| Java type           | Sérialisé en JSON                                  | Pour quoi                    |
|---------------------|----------------------------------------------------|------------------------------|
| `String`            | `"texte"`                                          | Texte                        |
| `int`, `long`       | nombre                                             | Quantité, threshold          |
| `BigDecimal`        | **éviter pour les montants** (parsé en double JS)  | -                            |
| `String` (montant)  | `"45.90"` via `.toPlainString()`                   | Argent                       |
| `UUID`              | `"9a8b7c6d-..."` format canonique                  | Identifiants                 |
| `Instant`           | `"2026-05-10T14:30:00Z"` ISO 8601 UTC              | Timestamps                   |
| `LocalDateTime`     | `"2026-05-10T14:30:00"` (PAS de Z, ambigu)         | À éviter en API              |
| `OffsetDateTime`    | `"2026-05-10T16:30:00+02:00"`                      | Quand le fuseau est métier   |
| enum                | `"VALEUR"` via `name()` en majuscule               | Valeurs fixes (status, type) |

Spring Boot 3 inclut le module `jackson-datatype-jsr310` qui sait sérialiser `Instant` et
les autres types `java.time.*` en ISO 8601 par défaut. Aucune config requise.

#### 8. Architecture hexagonale — placement des DTOs HTTP

Règle absolue : un concept HTTP (statut UP/DOWN, format de réponse, code d'erreur HTTP)
vit dans `infrastructure/web/`, **jamais** dans le domaine ou l'application.

- Le **domaine** s'occupe des règles métier (produits, ventes, stocks). Il ne connaît pas
  HTTP.
- L'**application** orchestre les use cases. Elle ne connaît pas HTTP.
- L'**infrastructure** contient l'adapter web (controllers, DTOs HTTP) et l'adapter de
  persistance (JPA). Elle dépend du domaine, jamais l'inverse.

Sens des flèches : **infrastructure → domaine**. Toujours. Si on regarde dans le
domaine et qu'on voit une dépendance vers Spring, JPA, Jackson, ou n'importe quelle libi
d'infra — c'est une violation à corriger.

#### 9. Tests web avec `@WebMvcTest`

`@WebMvcTest(MonController.class)` est un **slice test** : Spring ne charge que la
couche web (DispatcherServlet, Jackson, validation, le controller ciblé). Pas de JPA,
pas de DB, pas de use cases applicatifs. Rapide (sous la seconde), isolé.

Outils :
- `MockMvc` (auto-injecté) : simule une requête HTTP en mémoire, sans ouvrir de port.
- `MockMvcRequestBuilders.get(...)`, `post(...)`, etc. : construisent la requête.
- `MockMvcResultMatchers.status().isOk()`, `.contentType(...)`, `jsonPath("$.xxx").value(...)` :
  assertions.
- `@MockBean` : remplace un bean dans le contexte de test par un mock Mockito (essentiel
  quand le controller a des dépendances comme un repository).

#### 10. Différence `@WebMvcTest` vs `@SpringBootTest`

`@SpringBootTest` charge l'**intégralité** du contexte Spring : controllers, services,
repositories, JPA, sécurité. C'est lent (plusieurs secondes par test) mais représentatif
d'une exécution réelle. Pour les tests d'intégration end-to-end.

`@WebMvcTest` ne charge que la couche web — le DispatcherServlet, Jackson, le controller
ciblé. Rapide, isolé. Les dépendances du controller (use cases, repositories) sont
mockées via `@MockBean`. Pour la majorité des tests de controller.

Stratégie de test pyramide : beaucoup de tests unitaires (très rapides), quelques tests
de slice (`@WebMvcTest`, `@DataJpaTest`), peu de tests d'intégration end-to-end (lents
mais critiques).

#### 11. BOM Spring Boot et gestion des versions

Le BOM `spring-boot-dependencies` est importé en `<dependencyManagement>` du parent POM.
Il déclare des versions **cohérentes entre elles** pour toutes les bibliothèques de
l'écosystème Spring : Spring Core, Spring MVC, Hibernate, Jackson, Tomcat, JUnit, etc.

**Règle entreprise** : quand on utilise un BOM, on ne met jamais de `<version>` sur les
dépendances qu'il gère. Si on hardcode une version, on risque la divergence et des bugs
runtime difficiles à diagnostiquer.

#### 12. Starters Spring Boot

Un starter Spring Boot est une dépendance Maven **agrégée** qui tire transitivement un
ensemble cohérent d'autres dépendances.

`spring-boot-starter-web` contient :
- `spring-web` et `spring-webmvc` (DispatcherServlet, annotations, etc.) ;
- `spring-boot-starter-tomcat` (serveur Tomcat embarqué) ;
- `jackson-databind` et `jackson-datatype-jsr310` (sérialisation JSON, dont Instant) ;
- `jakarta.validation-api` (validation Bean Validation).

Une seule dépendance déclarée, tout l'écosystème web disponible. C'est l'idée
*"convention over configuration"*.

---

### Ticket 4.2 — `GET /api/v1/products/{productId}` (terminé)

#### 1. Pourquoi un DTO Response et pas l'objet métier

Le DTO HTTP est un **mur de protection** entre la couche HTTP et le domaine. Trois rôles :

- **Découplage** : le contrat HTTP peut rester stable même si le domaine évolue
  (renommage, refactor). Sans DTO, un renommage de `Product.minimumGlobalThreshold` en
  `Product.alertThreshold` casserait tous les clients HTTP.
- **Sécurité** : on liste explicitement les champs exposés. Pas de fuite involontaire
  (ex : un champ `internalCostPrice` qu'on ne veut pas exposer aux clients front).
- **Composition** : on peut assembler des données issues de plusieurs agrégats ou
  ajouter des champs calculés. Un `ProductStockSummary` peut combiner `Product` +
  somme de stock — alors qu'aucun agrégat domaine ne porte cette info.

#### 2. Pas de type domaine dans un DTO

Un DTO HTTP n'utilise que des **types primitifs ou standards** (`UUID`, `String`,
`Instant`, `int`) et d'autres DTOs. **Jamais** un type du domaine (`ProductId`,
`Money`, `Sale`).

Deux raisons :
- **Découplage** : si demain on renomme `ProductId` en `Sku` côté domaine, le DTO ne
  doit pas être impacté.
- **Sérialisation Jackson** : `UUID` est natif Jackson (→ string canonique). Si on
  mettait `ProductId`, Jackson sérialiserait `{ "value": "..." }` au lieu de la string
  nue, brisant le contrat OpenAPI. On pourrait corriger avec `@JsonValue` sur
  `ProductId` mais ça pollue le domaine avec Jackson.

Conclusion : le **mapper** (dans l'infrastructure) fait la traduction
`value object domaine → type standard` au moment de construire le DTO. Le DTO ne
connaît pas le domaine.

#### 3. Money en string — le piège JavaScript

Le format JSON ne distingue qu'un seul type "nombre". Quand un client JS lit un nombre,
il le parse en `IEEE 754 double`. Or les flottants ont une **précision limitée** :

```javascript
0.1 + 0.2  // → 0.30000000000000004
```

Pour de l'argent, c'est inacceptable. Solution standard (Stripe, PayPal, banques) :
**transporter le montant en string**, parser côté client avec une lib décimale.

Côté Java : on garde `BigDecimal` dans le domaine pour la précision serveur. Au moment
du mapping vers le DTO, on appelle `bigDecimal.toPlainString()` (et **pas** `.toString()`
qui peut produire de la notation scientifique).

#### 4. `MoneyResponse` comme value object

`Money` apparaît dans plusieurs DTOs (Product, Sale, SaleLine). On a créé un type dédié
`MoneyResponse(String amount, String currency)` plutôt que d'aplatir en
`unitPriceAmount`/`unitPriceCurrency` partout. Trois raisons :

- **Réutilisation** : un seul endroit pour la structure Money.
- **Sémantique** : reflète l'unité métier (un montant **et** sa devise indissociables).
- **Alignement OpenAPI** : le schéma `Money` est un `$ref` réutilisable, notre code doit
  refléter cette structure.

#### 5. Le découplage en profondeur — exemple Stripe

Stripe représente tous les montants côté API en **centimes entiers** (`amount: 1299` au
lieu de `"12.99"`). Aucun BigDecimal nulle part côté serveur, plus de précision flottante.

Le DTO te donne la liberté de **changer la représentation interne sans toucher au
contrat HTTP**. Tu pourrais passer ton `Money.amount` de `BigDecimal` à `long` (en
centimes) sans casser un seul client : le mapper absorberait la conversion
centimes → string décimal au moment de construire le `MoneyResponse`.

Formulation à mémoriser : *"On peut faire évoluer la représentation interne sans casser
le contrat publié — le mapper absorbe la différence. Inversement, on peut évoluer le
contrat HTTP (passer en /api/v2) sans toucher au domaine. C'est ce découplage qui permet
à une API mature de vivre longtemps malgré les refactors."*

#### 6. Mappers — statique ou bean Spring ?

**Méthode statique** dans une *utility class* :

```java
public final class ProductWebMapper {
    private ProductWebMapper() { /* pas d'instanciation */ }
    public static ProductResponse toResponse(Product p) { ... }
}
```

`final` interdit l'extension. Constructeur `private` interdit l'instanciation.

Quand utiliser quoi :
- **Pas de dépendance** (conversion pure) → méthode statique.
- **Dépendances Spring nécessaires** (autre mapper, service de traduction, Clock) →
  bean annoté `@Component`.

**MapStruct** est une alternative pour plus tard : on déclare une interface annotée
`@Mapper`, MapStruct **génère le code** du mapper à la compilation. Pratique quand on a
beaucoup de mappers, mais moins pédagogique pour débuter.

#### 7. Le concept fondamental de **bean Spring**

Un bean Spring est une **instance de classe** dont Spring a pris en charge la création
et le cycle de vie. Au démarrage, Spring scanne le code, trouve les classes annotées
d'une certaine façon, et **les instancie lui-même**. Ces instances sont stockées dans
l'**ApplicationContext** (le grand registre interne).

**Distinction cruciale à intégrer** : annotation ≠ bean.

- Annotations qui **créent** un bean (stéréotypes) : `@Component`, `@Service`,
  `@Repository`, `@RestController`, `@Controller`, `@Configuration`.
- Annotations qui ajoutent du **comportement** sans créer de bean : `@RequestMapping`,
  `@GetMapping`, `@PathVariable`, `@RequestBody`, `@Autowired`, `@Transactional`,
  `@Valid`, `@JsonProperty`, etc.

Toutes les annotations stéréotype héritent en fait de `@Component`. Les noms différents
(`@Service`, `@Repository`, etc.) servent à **documenter l'intention** et à activer du
comportement spécifique (Spring traite les `@Repository` pour la gestion d'exceptions
JPA, par exemple).

Formulation entretien : *"Un bean Spring est une instance de classe gérée par Spring :
créée, stockée dans l'ApplicationContext, injectable. Seules les annotations stéréotypes
créent un bean. Toutes les autres annotations Spring ajoutent du comportement ou des
métadonnées, sans créer d'objet par elles-mêmes."*

#### 8. Injection par constructeur > injection par champ

**Préféré** :

```java
@RestController
public class ProductController {
    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {  // pas besoin de @Autowired depuis Spring 4.3
        this.repository = repository;
    }
}
```

**À éviter en code de prod** :

```java
@Autowired private ProductRepository repository;  // injection par champ
```

Trois raisons d'utiliser le constructeur :

- **Immutabilité** : le champ peut être `final`. Le compilateur garantit qu'il sera
  initialisé une fois et jamais réassigné.
- **Testabilité** : instanciation directe sans Spring possible
  (`new ProductController(mockRepository)`). Avec injection par champ, on doit utiliser
  la réflexion ou de gros mocks Spring.
- **Dépendances explicites** : la signature du constructeur **liste** ce dont la classe
  a besoin. Lecture immédiate.

Injection par champ tolérée seulement dans les classes de test, par commodité.

#### 9. `@PathVariable` et le flag `-parameters`

`@PathVariable UUID productId` extrait un segment d'URL pour le passer en argument. Le
nom du paramètre (`productId`) doit matcher le nom dans le template
(`{productId}` dans `@GetMapping("/{productId}")`).

**Piège** : par défaut le compilateur Java **n'inclut pas les noms de paramètres** dans
le bytecode (héritage des années 90 où l'espace comptait). Spring ne peut pas faire le
binding par nom et lève une exception.

Solution : activer le flag `-parameters` dans le `maven-compiler-plugin` du parent POM :

```xml
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <parameters>true</parameters>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

Si on utilisait `spring-boot-starter-parent` comme parent (pas notre cas), ce flag serait
activé par défaut. Comme on utilise seulement le BOM, il faut le déclarer
explicitement.

**Note de cache** : après modification du pom, `mvn clean install -DskipTests` est
indispensable pour purger le cache `target/` et recompiler avec le nouveau flag.

#### 10. `ResponseEntity<T>` — quand l'utiliser

Quand le controller doit signaler **plusieurs statuts possibles** (200/404, par exemple),
on retourne `ResponseEntity<T>` plutôt que `T` directement. `ResponseEntity` est un
wrapper qui contient status + headers + body.

Factory methods principaux :

```java
ResponseEntity.ok(body)                  // 200 + body
ResponseEntity.notFound().build()        // 404 sans body
ResponseEntity.created(uri).body(body)   // 201 + Location header + body
ResponseEntity.accepted().body(body)     // 202 + body
ResponseEntity.noContent().build()       // 204 sans body
ResponseEntity.badRequest().build()      // 400
```

Pour un endpoint qui retourne **un seul statut** (toujours 200), on peut continuer à
retourner `T` directement (comme `/health` du ticket 4.1).

#### 11. `Optional<T>` en retour de repository

`Optional<T>` apporte trois choses qu'un retour direct (`T` qui peut être `null`)
n'apporte pas :

- **Type self-documenting** : la signature `Optional<Product> findById(...)` documente
  l'absence possible. Le caller le sait sans lire la doc.
- **Compilateur qui force le check** : impossible d'appeler `.getName()` directement sur
  un `Optional<Product>`. On doit passer par `.isPresent()`, `.get()`, `.map()`,
  `.orElseThrow()`. Le compilateur ne te laisse pas oublier la gestion d'absence.
- **Gestion locale de l'absence** : on traite l'absence là où elle est détectée, pas dix
  appels plus loin via NPE.

Pattern fonctionnel idiomatique :

```java
return productRepository.findById(id)
        .map(ProductWebMapper::toResponse)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
```

Lecture naturelle : *"trouve par id, si présent map en réponse, emballe en 200, sinon
construis un 404"*.

Recommandation Effective Java (item 55) : `Optional` en **retour** quand la non-présence
est attendue. **Jamais** comme paramètre, jamais comme champ.

#### 12. Inversion de dépendance (DIP) et archi hexagonale

Le domaine définit l'**interface** (port) : `ProductRepository`. L'infrastructure
**implémente** cette interface (adapter) : `ProductJpaRepositoryAdapter`.

Le `ProductController` (infrastructure) **dépend** du port (interface du domaine). C'est
OK. Le **domaine** ne dépend pas du controller, ni de l'adapter JPA — c'est ce qui
compte.

Sens des flèches : `infrastructure → domaine`. Toujours. Jamais l'inverse.

Spring se charge à l'injection de fournir la bonne implémentation : il voit que le
controller demande un `ProductRepository`, il trouve dans son contexte le bean qui
implémente cette interface (l'adapter JPA), il l'injecte. Magic.

Formulation entretien : *"Le domaine fournit un port — un contrat. C'est à l'extérieur
de s'adapter à ce contrat. Si on change d'infrastructure (passer de PostgreSQL à
MongoDB), le domaine n'est pas impacté."*

#### 13. CQRS pragmatique — lectures vs mutations

Convention adoptée pour ce projet :

- **Lectures simples** (récupérer un produit par ID) : le controller dépend
  directement du port repository. Pas de use case applicatif intermédiaire.
- **Mutations** (vendre, recevoir, transférer) : le controller construit un Command,
  appelle un use case applicatif, qui orchestre la logique métier.
- **Lectures enrichies** (jointures, agrégations cross-agrégats) : on introduira un
  query service ou un read-model dédié plus tard.

Argument entretien : *"CQRS pragmatique — les lectures simples vont directement au port
repository, les commandes passent par un use case applicatif. Quand une lecture devient
non triviale, on introduit un service de query."*

#### 14. Interagir avec PostgreSQL dans Docker

Connexion à la base via le container :

```bash
docker ps                                           # identifier le container
docker exec -it <container> psql -U <user> -d <db>  # session interactive
```

Commandes psql utiles :

- `\dt` : liste les tables
- `\d <table>` : décrit une table (colonnes, contraintes)
- `\q` : quitter
- requêtes SQL standard : `SELECT * FROM product;`

**Règle importante** : pas de données de seed dans Flyway. Les migrations Flyway
versionnent uniquement le **schéma** (DDL). Les seeds dev se font via script SQL
manuel ou via un `CommandLineRunner` Spring activé en profil `dev`.

#### 15. Maven multi-module

Structure :

```
auto-stock-management/   ← parent POM (type pom, agrégateur)
├── stock-domain/
├── stock-application/
└── stock-infrastructure/  ← contient StockApplication.main()
```

Commandes principales :

```bash
mvn clean install -DskipTests                       # recompile tout l'arbre
mvn -pl stock-infrastructure spring-boot:run        # lance l'app
mvn -pl stock-infrastructure -am spring-boot:run    # + recompile les modules dépendants
mvn -pl stock-infrastructure test                   # tests d'un module
```

`-pl` = `--projects`, cible un module. `-am` = `--also-make`, ajoute les modules dont le
module ciblé dépend.

**Piège** : lancer `mvn spring-boot:run` à la racine échoue avec *"Unable to find a
suitable main class"* — le parent POM n'a pas de classe `main`, il faut cibler le module
infra.

#### 16. Git — commits atomiques et rebase interactif

**Règle d'or** : un commit = une intention cohérente, qui peut être lue/comprise/revertée
seule. Si on doit décrire le commit avec un "et", on devrait probablement en faire deux.

**`git commit --amend`** : modifie le **dernier** commit (avant push). Utile pour
corriger une typo ou ajouter un fichier oublié.

**`git rebase -i HEAD~N`** : modifie l'histoire sur les N derniers commits. Permet de
réordonner, fusionner (squash), modifier un commit antérieur (edit), changer un message
(reword). Outil le plus puissant et le plus dangereux de Git.

**Règle absolue** : on **ne rebase jamais** un commit qui a été poussé sur une branche
partagée. Rebase réécrit les hashes — push après rebase casse l'historique distant et
déphase tous les collègues. Catastrophe.

En cas de panique pendant un rebase : `git rebase --abort` annule et remet exactement
comme avant.

#### 17. Conventions Conventional Commits

Format :

```
<type>(<scope>): <description impérative présente>
```

Types : `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`, `build`, `style`.

Scopes pour ce projet : `domain`, `application`, `infrastructure`, `web`, `persistence`,
`config`, `project`, `build`.

Description : impératif présent (*"add product DTO"* pas *"added"* ni *"adding"*),
anglais, minuscule, pas de point final. Comme si on complétait *"If applied, this commit
will..."*.

Exemples conformes :

```
feat(web): add MoneyResponse DTO
fix(web): rename ProductResponse.category to categoryId per OpenAPI
refactor(persistence): extract product mapper to dedicated class
chore(build): enable -parameters flag for maven compiler
```

#### 18. Convention de nommage des branches

Format : `<type>/<short-kebab-case-description>`

Exemples : `feat/get-product-by-id`, `fix/health-controller-missing-rest-annotation`,
`refactor/extract-product-mapper`, `chore/upgrade-spring-boot-3.5.13`.

Court, descriptif, anglais, kebab-case. Pas de préfixe utilisateur.

---

## Pièges récurrents à éviter (compilation)

- **`@RestController` oublié** → 404 silencieux. Pas d'erreur démarrage.
- **DTO Money en `number` JSON** → perte de précision en JavaScript.
- **DTO avec type domaine** (`ProductId` au lieu de `UUID`) → contrat couplé, Jackson
  sérialise `{"value": "..."}` au lieu de string nue.
- **Cache Maven non purgé** → après changement de pom, toujours `mvn clean install`.
- **`mvn spring-boot:run` à la racine** → "no main class". Cibler le module avec `-pl`.
- **Direct push sur `main`** → pas de revue, pas de PR, casse les normes entreprise.
- **`@PathVariable` sans `-parameters`** → IllegalArgumentException sur le binding.
- **Test avec `@SpringBootTest` quand `@WebMvcTest` suffit** → tests lents et fragiles.

---

## Commandes utiles à connaître par cœur

```bash
# Maven
mvn clean install -DskipTests                       # recompile tout
mvn -pl stock-infrastructure spring-boot:run        # lance l'app
mvn -pl stock-infrastructure test                   # tests d'un module
mvn dependency:tree                                  # voir l'arbre de dépendances

# Git
git status                                          # état du working directory
git log --oneline -10                               # historique court
git diff                                            # voir les modifications non staged
git add <fichier>                                   # stage
git commit -m "feat(scope): description"            # commit
git commit --amend --no-edit                        # amender le dernier commit
git rebase -i HEAD~N                                # rebase interactif sur N commits
git rebase --abort                                  # annuler un rebase en cours
git stash push -u -m "WIP"                          # mettre de côté
git stash pop                                       # récupérer le stash
git branch --show-current                           # branche courante

# Docker / Postgres
docker ps                                           # containers actifs
docker exec -it <container> psql -U <user> -d <db>  # session psql

# Curl pour tester l'API
curl -i http://localhost:8080/api/v1/health
curl -i http://localhost:8080/api/v1/products/<uuid>
```

---

## Formulations entretien-ready (compilation)

À mémoriser et resservir en entretien.

### Cycle d'une requête HTTP dans Spring

> "Quand un client envoie une requête HTTP, Tomcat embarqué la reçoit et la passe au
> DispatcherServlet. Le DispatcherServlet consulte sa table de routage construite au
> démarrage en scannant les classes `@RestController`. Il trouve la méthode qui matche
> exactement le couple verbe HTTP + chemin, l'invoque, récupère l'objet retourné, le
> sérialise en JSON via Jackson grâce à la réflexion, et renvoie la réponse au client."

### `@WebMvcTest` vs `@SpringBootTest`

> "`@WebMvcTest` ne charge que la couche web — DispatcherServlet, Jackson, controller
> ciblé — avec mocks pour les dépendances. C'est rapide et isolé. `@SpringBootTest`
> charge tout le contexte, c'est lent mais représentatif. On utilise les slice tests
> pour la majorité, l'intégration pour les chemins critiques."

### Versioning d'API

> "Le préfixe `/api/v1` permet de faire coexister plusieurs versions du contrat HTTP.
> Quand on doit casser le contrat, on publie `/api/v2` en parallèle, on annonce une
> sunset date pour `v1`, et on supprime quand tous les clients ont migré. C'est la
> mécanique standard pour faire évoluer une API publique sans rupture de service."

### Pourquoi un DTO et pas l'objet métier

> "Le DTO est un mur de protection entre la couche HTTP et le domaine. Premièrement, il
> découple — le contrat de l'API peut rester stable même si le domaine évolue. Deuxièmement,
> il sécurise — on liste explicitement les champs exposés, sans risque de fuite involontaire.
> Troisièmement, il compose — on peut assembler des données issues de plusieurs agrégats
> ou ajouter des champs calculés que le domaine ne porte pas."

### Découplage représentation interne / contrat HTTP

> "Le DTO permet d'évoluer indépendamment dans les deux sens. On peut changer la
> représentation interne (passer de BigDecimal à un entier de centimes pour simplifier
> le code) sans casser le contrat publié, parce que le mapper absorbe la différence.
> Inversement, on peut faire évoluer le contrat HTTP sans modifier le domaine. C'est ce
> découplage qui permet à une API mature de vivre longtemps malgré les refactors."

### Bean Spring vs annotation

> "Un bean Spring est une instance de classe gérée par Spring — créée, stockée dans
> l'ApplicationContext, injectable dans d'autres beans. Seules les annotations
> stéréotypes (`@Component`, `@Service`, `@Repository`, `@RestController`,
> `@Configuration`) créent un bean. Toutes les autres annotations Spring ajoutent du
> comportement ou des métadonnées, sans créer d'objet par elles-mêmes."

### Injection par constructeur

> "On préfère l'injection par constructeur à l'injection par champ pour trois raisons :
> les champs peuvent être `final` (immutabilité), la classe est instanciable sans Spring
> (testabilité directe avec des mocks), et la signature du constructeur documente
> explicitement les dépendances. L'injection par champ avec `@Autowired` est tolérée
> seulement dans les classes de test."

### Optional vs null

> "Retourner `Optional<T>` plutôt que `T` ou `null` apporte trois choses. La signature
> documente l'absence possible — le caller le sait sans lire la doc. Le compilateur
> force la gestion d'absence via l'API Optional, le caller ne peut pas oublier le check.
> La gestion d'absence se fait à l'endroit où elle est détectée, pas dix appels plus
> loin via NullPointerException. C'est la recommandation d'Effective Java item 55 :
> Optional en retour quand la non-présence est attendue, jamais comme paramètre ni
> comme champ."

### Architecture hexagonale (DIP)

> "Le domaine fournit un port — un contrat — par lequel l'extérieur communique avec lui.
> C'est à l'extérieur de s'adapter à ce contrat. L'infrastructure (controllers,
> adapters JPA) dépend du domaine via ses interfaces ; le domaine ne dépend de rien.
> Si on change d'infrastructure — par exemple passer de PostgreSQL à MongoDB — le
> domaine n'est pas impacté."

### CQRS pragmatique

> "Les lectures simples vont directement au port repository depuis le controller, sans
> passer par un use case applicatif. Les mutations passent par un use case qui orchestre
> la logique métier. Quand une lecture devient non triviale — jointures, agrégations
> cross-agrégats — on introduit un service de query ou un read-model dédié."

### Money en string

> "Tous les montants monétaires sont transportés en JSON sous forme de string, pas de
> number. C'est parce que `number` en JavaScript est un IEEE 754 double avec une
> précision limitée. Pour de l'argent c'est inacceptable — on peut avoir des écarts de
> centimes. Toutes les API financières sérieuses, comme Stripe ou les banques,
> transportent en string. Côté serveur, on garde BigDecimal pour la précision, et le
> mapper appelle `.toPlainString()` au moment de construire le DTO."

---

_Fichier à enrichir à chaque ticket terminé. Relire chaque matin 10 minutes, reformuler à voix haute._
