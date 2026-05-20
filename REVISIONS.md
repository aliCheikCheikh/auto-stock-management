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

### Ticket 4.3 — `GET /api/v1/products` paginé (terminé)

#### 1. La pagination — concept et raisons

Sans pagination, un endpoint de liste retournerait **toutes** les ressources d'un coup. À
l'échelle d'un magasin avec 5 000 produits, c'est 5 MB de JSON par requête, plusieurs
secondes de latence réseau, et un client navigateur ou mobile qui rame ou crashe. La
pagination découpe la collection en pages successives, le client demande seulement ce
dont il a besoin (et navigue page par page si nécessaire).

Deux conventions principales coexistent :

- **Offset-based** (`?offset=20&limit=10`) : SQL natif. Simple à implémenter, mais devient
  lent et incohérent quand la table grandit ou quand des insertions ont lieu pendant la
  navigation.
- **Page-based** (`?page=1&size=10`) : convention REST classique. Dérivée de l'offset-based
  (`offset = page * size`). C'est ce qu'on a choisi pour ce projet.
- **Cursor-based** (`?after=<token>`) : pour des collections très grandes ou en mutation
  rapide (feed Twitter, logs). On y reviendra plus tard pour `/stock-movements`.

#### 2. `@RequestParam` vs `@PathVariable`

Les deux annotations existent côte à côte, mais elles lisent **deux endroits différents** de
l'URL :

- `@PathVariable` lit un **segment de chemin** (entre slashes). URL `/api/v1/products/{id}`
  → le `id` fait partie du chemin lui-même.
- `@RequestParam` lit un **paramètre de query string** (après le `?`). URL
  `/api/v1/products?page=0&size=20` → les `page` et `size` sont après le point d'interrogation.

Côté code :

```java
@GetMapping("/{productId}")
public ResponseEntity<ProductResponse> getOne(@PathVariable UUID productId) { ... }

@GetMapping
public ResponseEntity<PageOfProductResponse> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
) { ... }
```

#### 3. `defaultValue` pour des params optionnels

Sans `defaultValue`, un `@RequestParam` est **obligatoire** par défaut. Si le client n'envoie
pas le param, Spring renvoie 400. Avec `defaultValue = "..."`, le param devient optionnel et
Spring substitue la valeur fournie quand le client ne l'envoie pas.

Subtilité importante : `defaultValue` est **toujours une string**, même pour un `int`. C'est
cohérent avec HTTP — tout dans l'URL est texte. Spring fait la conversion vers le type Java
après. Si tu mets `defaultValue = "abc"` sur un `int`, l'app refuse de démarrer (Spring teste
les conversions au boot).

#### 4. Wrapper DTO custom vs `Page<T>` de Spring Data

`JpaRepository.findAll(Pageable)` retourne un `Page<T>` qui contient le contenu + des
métadonnées. Si on sérialise ce `Page<T>` direct en JSON, Jackson produit quelque chose
comme :

```json
{
  "content": [...],
  "pageable": { "sort": {...}, "offset": 0, "pageNumber": 0, ... },
  "totalElements": 25,
  "totalPages": 2,
  "last": false,
  "size": 20,
  "number": 0,
  "first": true,
  "numberOfElements": 20,
  "empty": false
}
```

Verbeux, redondant (`size` vs `pageable.pageSize`, `number` vs `pageable.pageNumber`), et
expose la structure interne Spring Data au client. Si demain on change de stack persistance,
le contrat HTTP casse.

On crée donc **nos propres DTOs** alignés sur le contrat OpenAPI :

```java
public record PageOfProductResponse(
        List<ProductResponse> content,
        PageMetaResponse page
) { }

public record PageMetaResponse(
        int page,
        int size,
        long totalElements,
        int totalPages
) { }
```

JSON résultat : minimal, prévisible, indépendant de Spring Data.

#### 5. Port domaine — décision `page/size` vs `offset/limit`

Vrai débat architectural. Le port domaine peut être nommé selon le vocabulaire pur SQL
(`offset/limit`) ou selon la convention REST (`page/size`).

- **`offset/limit`** : agnostique de l'API exposée, vocabulaire SQL universel. Plus pur
  d'un point de vue hexagonal.
- **`page/size`** : cohérent avec l'API REST et avec Spring Data. L'adapter JPA fait
  trivialement `PageRequest.of(page, size)` sans calcul.

**Choix pour ce projet : `page/size`**. Pragmatisme : pas d'arithmétique dans l'adapter,
pas de risque d'erreur quand `offset` n'est pas un multiple de `limit`, cohérence avec la
convention HTTP. Le port reste 100% POJO (pas de type Spring), donc le couplage est purement
nominal.

Argument à défendre en entretien : *"Le port utilise `page/size` par pragmatisme, cohérent
avec la convention HTTP. L'alternative `offset/limit` serait plus pure (vocabulaire SQL)
mais ajouterait du calcul dans l'adapter sans bénéfice pratique."*

#### 6. Bean Validation côté HTTP — `@Validated` et le piège du 500

Bean Validation est la spec Java standard (`jakarta.validation`) qui déclare des contraintes
sur des champs ou paramètres via annotations. Hibernate Validator est l'implémentation par
défaut (incluse dans `spring-boot-starter-web`).

Pour valider des query params, deux annotations s'empilent :

```java
@RestController
@RequestMapping("/api/v1/products")
@Validated                                                  // ← Spring, niveau classe
public class ProductController {

    @GetMapping
    public ResponseEntity<PageOfProductResponse> getAllProducts(
            @RequestParam @Min(0) int page,                  // ← Jakarta, sur le param
            @RequestParam @Min(1) @Max(200) int size
    ) { ... }
}
```

`@Validated` (Spring) au niveau classe **active** la validation des paramètres de méthode.
Sans elle, les annotations `@Min`/`@Max` posées sur les paramètres sont **silencieusement
ignorées**.

**Piège majeur à connaître par cœur** : différence entre `@Valid` et `@Validated`.

- `@Valid` (Jakarta) sur un `@RequestBody` → violation lève `MethodArgumentNotValidException`
  → mappée automatiquement par Spring en **400**.
- `@Validated` (Spring) sur les paramètres → violation lève `ConstraintViolationException`
  → mappée par Spring en **500 par défaut**. Il faut un handler explicite pour ramener à 400.

C'est une asymétrie historique de Spring que tout backend Java doit connaître. Sans gérer,
ton endpoint répond 500 sur une erreur client — comportement incorrect (5xx = bug serveur,
4xx = erreur client).

#### 7. `@ExceptionHandler` local pour mapper une exception en réponse

Solution la plus simple pour ramener `ConstraintViolationException` à 400, dans le controller
lui-même :

```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<Void> handleConstraintViolation(ConstraintViolationException e) {
    return ResponseEntity.badRequest().build();
}
```

Spring intercepte les exceptions levées par les méthodes du controller, cherche un
`@ExceptionHandler` matchant le type d'exception, et utilise sa valeur de retour comme
réponse HTTP.

C'est un **handler local** (limité à ce controller). Au Chantier 5, on remplacera par un
`@RestControllerAdvice` global qui couvre toute l'app et utilise `ProblemDetail` RFC 7807
pour des réponses d'erreur structurées.

#### 8. Pagination math — calcul de `totalPages`

Formule simple :

```java
int totalPages = (int) Math.ceil((double) totalElements / size);
```

Piège classique à éviter : utiliser `content.size()` au lieu de `totalElements`.
`content.size()` est le nombre d'éléments **dans la page courante** (jamais > `size`).
`totalElements` est le nombre **total** d'éléments dans la collection. Pour calculer le
nombre de pages, c'est `totalElements` qu'il faut, sinon le résultat dépend de la page
demandée — absurde.

Cas particulier : si `totalElements == 0`, `Math.ceil(0.0 / size) == 0`. Donc `totalPages == 0`.
C'est cohérent avec une collection vide : zéro page.

#### 9. TDD discipline complète — appliquée sur ce ticket

Premier ticket où on a fait du TDD strict de bout en bout. Cycle :

1. **Red** : écrire un test qui échoue (compile pas ou exécution rouge).
2. **Green** : écrire le minimum de code de production pour faire passer le test.
3. **Refactor** : améliorer le code en gardant le test au vert.
4. **Commit** : capture du cycle.

Règles intégrées sur ce ticket :

- Un seul test par cycle. Test trop ambitieux → cycle trop long → on découpe.
- Le test pilote la signature des méthodes du domaine et la forme du JSON exposé.
- **Prédiction TDD** avant chaque test : *"ce test va passer ou échouer ?"*. Construit
  l'intuition sur son propre code.
- Commit après chaque cycle vert (et pas à la fin) — sinon on accumule et on doit faire du
  `git add -p` pour reconstituer l'atomicité.

Bénéfice constaté : sur 6 cycles, **3 tests sont passés du premier coup** parce que le code
de prod du cycle précédent les couvrait déjà. Ces "tests gratuits" sont des régression tests
acquis sans effort.

#### 10. Test data avec `IntStream` + helper method

Quand un test a besoin de plusieurs entités (20, 100, etc.), on évite la liste hardcodée.
Pattern utilisé :

```java
private Product sampleProduct(int index) {
    UUID productId = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", index));
    return new Product(
            ProductId.of(productId),
            "Product " + index,
            "REF-" + String.format("%03d", index),
            CategoryId.of(...),
            10,
            Money.create(new BigDecimal("45.90"), Currency.getInstance("EUR"))
    );
}

// Utilisation
List<Product> twentyProducts = IntStream.range(0, 20)
        .mapToObj(this::sampleProduct)
        .toList();
```

UUID prédictibles via `String.format("%012d", index)` : pratique si un jour il faut asserter
sur un UUID précis. Helper privé : factorise sans dépendre d'un framework de test data
externe.

À retenir : pour des tests plus complexes, on évoluera vers un **Test Data Builder** dédié
(pattern `aProduct().withName(...).build()`) ou un **Object Mother**.

#### 11. `.param(...)` de MockMvc pour les query params

Deux syntaxes possibles pour ajouter des query params dans un test :

```java
mockMvc.perform(get("/api/v1/products?page=1&size=10"))             // concaténation
mockMvc.perform(get("/api/v1/products")
        .param("page", "1")
        .param("size", "10"))                                       // .param() — préféré
```

Les deux marchent, mais `.param(...)` est plus lisible et extensible (5+ params restent
lisibles). Valeurs toujours en string (HTTP est du texte).

#### 12. Commits atomiques avec `git add -p`

Quand on accumule plusieurs modifications dans le même fichier (typiquement plusieurs tests
ou tests + code de prod) avant de commit, `git add -p` permet de stager **morceau par
morceau** (hunk par hunk) :

```bash
git add -p <fichier>
# Pour chaque hunk affiché, répondre :
#   y → stager
#   n → ne pas stager
#   s → split (diviser le hunk en plus petits)
#   e → edit (modifier manuellement)
#   q → quitter
```

Permet de reconstituer des commits atomiques après-coup. Outil de rattrapage quand on a
oublié de commit après chaque cycle TDD.

Discipline préférée : commit immédiatement après chaque cycle vert, pour ne pas avoir à
faire du `git add -p` à la fin.

---

### Ticket 4.5 — `POST /api/v1/sales` (terminé)

#### 1. Lire OpenAPI avec les `$ref`

Dans OpenAPI, le bloc de l'endpoint ne contient pas toujours le détail complet du body ou
de la réponse. Il pointe souvent vers un schéma réutilisable :

```yaml
schema:
  $ref: '#/components/schemas/SaleResponse'
```

Ça veut dire : *"va lire `components.schemas.SaleResponse` plus bas dans le fichier"*.
Méthode de lecture :

1. Lire `paths./sales.post` pour connaître le verbe, le chemin, les statuts et les refs.
2. Lire `CreateSaleRequest` pour déduire le DTO HTTP de requête.
3. Lire `CreateSaleLine` pour déduire le DTO imbriqué.
4. Lire `SaleResponse` pour déduire le DTO HTTP de réponse.
5. Lire `SaleLineResponse` pour déduire chaque ligne de réponse.

Conclusion du contrat :

- request : `sellerId`, `shopId`, `lines[]` avec `productId` et `quantity` ;
- response : `saleId`, `sellerId`, `lines`, `totalAmount`, `createdAt` ;
- ligne de response : `productId`, `quantity`, `unitPrice`, `subtotal`.

#### 2. Pourquoi `SellProductUseCase` retourne `SellProductResult`

Comme pour la réception de stock, le controller ne peut pas construire une réponse HTTP
complète si le use case retourne `void`.

Pour `POST /sales`, la réponse OpenAPI demande au minimum :

- l'ID de la vente ;
- le vendeur ;
- les lignes vendues ;
- le total ;
- la date de création.

Ces informations existent dans l'objet domaine `Sale`, créé par le use case. Le controller
ne doit pas aller les chercher dans un repository ni reconstruire une vente lui-même.
Solution : faire retourner un DTO applicatif :

```java
public record SellProductResult(
        SaleId saleId,
        UserId sellerId,
        List<SaleLineDto> lines,
        Money totalAmount,
        LocalDateTime createdAt
) {
}
```

Le result applicatif peut utiliser des types domaine (`SaleId`, `UserId`, `Money`) parce
qu'il reste dans la couche application. Le DTO HTTP convertira ensuite vers des types
standards (`UUID`, `String`, `int`, etc.).

#### 3. `createdAt` : utiliser le timestamp métier, pas `now()`

Erreur tentante : faire `LocalDateTime.now()` ou `Instant.now()` au moment de construire
`SellProductResult`.

Ce serait moins correct, car on créerait un second timestamp :

```text
sale.getOccurredAt()       -> moment métier où la vente a été créée
LocalDateTime.now() après  -> moment technique où on construit le result
```

La réponse HTTP doit refléter la vente créée, donc :

```java
result.createdAt() == savedSale.getOccurredAt()
```

À retenir : si l'objet domaine possède déjà le timestamp métier, le result doit le réutiliser.
On n'invente pas un nouveau temps dans le use case.

#### 4. `LocalDateTime.toString()` vs JSON Jackson

Dans un test, on a vu :

```text
expected: 2026-05-15T10:30
actual:   2026-05-15T10:30:00
```

Raison : `LocalDateTime.toString()` peut omettre les secondes quand elles valent zéro, alors
que Jackson sérialise le JSON avec les secondes.

Pour un test déterministe, on fixe la date :

```java
createdAt = LocalDateTime.of(2026, 5, 15, 10, 30);
```

Et on assert le JSON réellement produit :

```java
jsonPath("$.createdAt").value("2026-05-15T10:30:00")
```

Ne pas utiliser `LocalDateTime.now()` dans un test de controller : la valeur bouge, donc le
test devient fragile.

#### 5. `201 Created` vs `202 Accepted`

`POST /sales` retourne `201 Created` parce qu'une ressource vente est créée immédiatement.
L'OpenAPI annonce même un header `Location` vers la ressource créée.

`POST /stock-receipts` retournait `202 Accepted` parce que le contrat disait que la réception
était acceptée et que les mouvements étaient créés, sans ressource receipt consultable.

Règle simple :

- `201 Created` : la ressource principale est créée maintenant (`Sale`) ;
- `202 Accepted` : la requête est acceptée, souvent avec une logique plus asynchrone ou sans
  ressource directement exposée.

#### 6. `lineTotal` domaine -> `subtotal` HTTP

Le domaine utilise `SaleLineDto.lineTotal()`. L'OpenAPI expose le champ JSON `subtotal`.

Le DTO HTTP doit suivre OpenAPI, pas le vocabulaire interne du domaine :

```text
SaleLineDto.lineTotal() -> SaleLineResponse.subtotal()
```

Le mapper web absorbe cette différence de vocabulaire. C'est exactement son rôle :
traduire entre modèle applicatif/domaine et contrat HTTP public.

#### 7. Validation du body de vente

Validations ajoutées par cycles TDD :

- body vide -> 400 via `@RequestBody` ;
- `sellerId` absent -> 400 via `@Valid` + `@NotNull` ;
- `lines` vide -> 400 via `@NotEmpty` ;
- `quantity <= 0` -> 400 via `@Positive` sur la ligne ;
- validation imbriquée -> `@Valid` sur la liste `lines`.

Différence importante :

- `@NotNull` sur une liste vérifie seulement que la liste n'est pas `null` ;
- `@NotEmpty` vérifie que la liste n'est pas `null` **et** contient au moins un élément ;
- `@Valid` sur la liste permet de descendre dans chaque ligne pour vérifier `@Positive`.

#### 8. Test multi-lignes

Le cycle multi-lignes n'ajoute pas forcément de code si le mapper utilise déjà :

```java
request.lines().stream()
        .map(SaleWebMapper::toLineCommand)
        .toList()
```

Mais le test reste utile : il protège contre une régression où le mapper ne traiterait que
la première ligne.

Le test vérifie deux choses :

- la réponse contient bien `lines[0]` et `lines[1]` ;
- la `SellProductCommand` capturée contient bien deux `SellLineCommand`.

À retenir : un test qui passe directement peut être un bon test de non-régression.

---

### Ticket 4.6 — `POST /api/v1/stock-transfers` (terminé)

#### 1. Lire un endpoint avec un acknowledgement minimal

Pour `POST /stock-transfers`, OpenAPI indique :

- request : `productId`, `sourceLocationId`, `destinationLocationId`, `quantity`, `userId` ;
- response `202 Accepted` : `movementId`, `acceptedAt`.

La réponse ne contient pas le produit, les emplacements ou la quantité. Donc le controller
n'a pas besoin de recharger tout le transfert : il doit seulement recevoir du use case
l'identifiant du mouvement créé et le moment d'acceptation.

#### 2. Pourquoi `TransferStockUseCase` ne doit plus retourner `void`

Le use case crée un `StockMovement` de type `TRANSFER`. Le controller doit renvoyer son
`movementId`, mais il ne doit pas aller fouiller dans le repository pour le retrouver.

Solution :

```java
public record TransferStockResult(
        MovementId movementId,
        Instant acceptedAt
) {
}
```

Le use case reste responsable de l'action métier et retourne le minimum nécessaire au
caller. Le controller reste responsable du HTTP et transforme ce result en DTO de réponse.

#### 3. Validation HTTP simple sur un body plat

Le body de transfert n'a pas de liste imbriquée, donc pas besoin de `@Valid` sur une
collection comme pour `lines` ou `distributions`.

On utilise :

- `@NotNull` sur les UUID obligatoires ;
- `@Positive` sur `quantity` ;
- `@Valid @RequestBody` dans le controller pour déclencher Bean Validation.

À retenir : si `quantity` est un `int` primitif et absent du JSON, Jackson le met à `0`.
`@Positive` transforme donc naturellement ce cas en `400 Bad Request`.

#### 4. Test controller : `verifyNoInteractions` protège la frontière

Dans les tests de validation web, on vérifie toujours :

```java
verifyNoInteractions(transferStockUseCase);
```

Ça prouve qu'une requête invalide est bloquée dans la couche web. Le use case ne doit pas
recevoir une commande partiellement invalide.

#### 5. Maven multi-module et tests ciblés

Quand `stock-infrastructure` dépend d'une modification récente dans `stock-application`,
il faut parfois lancer avec `-am` (*also make*) :

```bash
mvn -pl stock-infrastructure -am -Dtest=StockTransferControllerTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
```

Pourquoi `-Dsurefire.failIfNoSpecifiedTests=false` ? Parce que `-am` lance aussi les modules
dépendants. `stock-domain` et `stock-application` n'ont pas forcément un test nommé
`StockTransferControllerTest`, et Surefire échoue sinon avant d'arriver au module web.

---

### Ticket 4.7 — `GET /api/v1/stock-movements` (terminé)

#### 1. Le but métier de l'endpoint

Les tickets précédents créent des mouvements de stock :

```text
POST /stock-receipts   -> crée des mouvements ENTRY
POST /sales            -> crée des mouvements EXIT
POST /stock-transfers  -> crée un mouvement TRANSFER
```

`GET /stock-movements` sert à lire cet historique. C'est la page que le frontend utiliserait
pour afficher : *"qu'est-ce qui s'est passé sur le stock ?"*

Exemple d'appel :

```http
GET /api/v1/stock-movements?page=0&size=20&type=TRANSFER&sort=executedAt,desc
```

Traduction humaine :

```text
Donne-moi les 20 premiers mouvements,
seulement les transferts,
triés du plus récent au plus ancien.
```

#### 2. Pourquoi un query use case ?

Pour `GET /products`, on avait fait simple :

```text
ProductController -> ProductRepository
```

Pour `stock-movements`, la lecture est plus riche :

- pagination ;
- tri ;
- filtre par produit ;
- filtre par emplacement ;
- filtre par type de mouvement ;
- filtre par période ;
- mapping entre noms API (`executedAt`) et noms JPA (`occurredAt`).

On a donc créé un use case de lecture :

```java
public class ListStockMovementsUseCase {
    public PageResult<StockMovementView> execute(ListStockMovementsQuery query) {
        return stockMovementQueryPort.findByQuery(query);
    }
}
```

Même s'il paraît petit, il donne une frontière claire :

```text
Controller HTTP
-> construit une query application
-> appelle le use case
-> le use case appelle un port
-> l'infrastructure JPA implémente ce port
```

Phrase à retenir : un controller ne doit pas devenir un endroit où on construit des requêtes
JPA complexes.

#### 3. `ListStockMovementsQuery` : regrouper les critères de recherche

Au lieu de passer 8 paramètres partout :

```java
find(page, size, sort, productId, locationId, type, from, to)
```

on les regroupe dans un seul objet :

```java
public record ListStockMovementsQuery(
        int page,
        int size,
        List<String> sort,
        ProductId productId,
        LocationId locationId,
        MovementType type,
        LocalDateTime from,
        LocalDateTime to
) {
}
```

Exemple :

```http
GET /stock-movements?page=1&size=10&type=EXIT
```

devient :

```java
new ListStockMovementsQuery(
        1,
        10,
        List.of("executedAt,desc"),
        null,
        null,
        MovementType.EXIT,
        null,
        null
)
```

Pourquoi `ProductId` et `LocationId` ici, alors que le HTTP reçoit des `UUID` ? Parce qu'on
est dans la couche application. Le HTTP connaît les `UUID`; l'application peut manipuler les
value objects du domaine pour être plus expressive.

#### 4. `PageResult<T>` : éviter de dépendre de Spring Data dans l'application

Spring Data fournit `Page<T>`, mais c'est un type du framework. Si on l'utilise dans
`stock-application`, on colle la couche application à Spring Data.

On a donc créé notre page neutre :

```java
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
```

Exemple :

```java
new PageResult<>(
        List.of(movement1, movement2),
        0,
        20,
        42,
        3
)
```

Traduction :

```text
Je suis sur la page 0.
J'ai demandé 20 éléments par page.
Il y a 42 éléments au total.
Donc il y a 3 pages.
Cette page contient movement1 et movement2.
```

À retenir : `PageResult<T>` est à l'application ce que `Page<T>` est à Spring Data.

#### 5. `StockMovementQueryPort` : contrat de lecture

Le port dit ce dont l'application a besoin :

```java
public interface StockMovementQueryPort {
    PageResult<StockMovementView> findByQuery(ListStockMovementsQuery query);
}
```

Traduction humaine :

```text
J'ai besoin de quelqu'un capable de chercher des mouvements de stock avec ces critères.
Je ne veux pas savoir si c'est fait avec JPA, SQL, Elasticsearch ou autre.
```

L'application dépend donc de l'interface, pas de JPA :

```text
ListStockMovementsUseCase -> StockMovementQueryPort
```

L'infrastructure fournit l'implémentation :

```text
StockMovementQueryJpaAdapter implements StockMovementQueryPort
```

#### 6. Est-ce du CQRS ?

Oui, mais en version pragmatique.

CQRS veut dire : séparer les commandes qui modifient le système des queries qui lisent le
système.

Dans notre cas :

```text
Command side :
POST /stock-receipts
POST /sales
POST /stock-transfers

Query side :
GET /stock-movements
```

Ce n'est pas du CQRS complet avec base de lecture séparée, event sourcing ou projections
asynchrones. C'est simplement une séparation propre :

```text
écriture -> use cases métier
lecture  -> query use case + query port
```

#### 7. `@RequestParam MultiValueMap<String, String>` : lire les query params bruts

Un paramètre HTTP peut apparaître plusieurs fois :

```http
GET /stock-movements?sort=executedAt,desc&sort=quantity,asc&type=TRANSFER
```

Spring peut représenter ça comme une map où chaque clé a une liste de valeurs :

```java
{
    "sort": ["executedAt,desc", "quantity,asc"],
    "type": ["TRANSFER"]
}
```

C'est exactement le rôle de :

```java
@RequestParam MultiValueMap<String, String> queryParams
```

Pourquoi ne pas écrire directement ?

```java
@RequestParam(required = false) List<String> sort
```

Parce que Spring peut découper `sort=executedAt,desc` en :

```java
["executedAt", "desc"]
```

Or nous voulons garder une instruction complète :

```java
["executedAt,desc"]
```

Donc on lit le param brut :

```java
queryParams.get("sort")
```

#### 8. `Sort`, `Sort.Order` et traduction API -> JPA

Le client parle avec les noms OpenAPI :

```http
GET /stock-movements?sort=executedAt,desc
```

Mais l'entity JPA n'a pas un champ `executedAt`. Elle a :

```java
private LocalDateTime occurredAt;
```

On doit donc traduire :

```text
API          JPA
movementId -> id
type       -> movementType
executedBy -> performedBy
executedAt -> occurredAt
```

Exemple :

```java
toOrder("executedAt,desc")
```

fait :

```text
split -> ["executedAt", "desc"]
toJpaProperty("executedAt") -> "occurredAt"
direction -> DESC
résultat -> Sort.Order.desc("occurredAt")
```

En SQL, ça revient à :

```sql
order by occurred_at desc
```

Si le client ne fournit aucun tri, on choisit le défaut métier :

```java
Sort.by(Sort.Order.desc("occurredAt"))
```

Donc l'historique affiche les mouvements les plus récents d'abord.

#### 9. `Specification` JPA : construire un `WHERE` dynamique

Le client peut envoyer zéro, un ou plusieurs filtres :

```http
GET /stock-movements
GET /stock-movements?productId=...
GET /stock-movements?productId=...&type=TRANSFER&from=2026-05-01T00:00:00
```

On ne veut pas écrire une méthode repository pour chaque combinaison :

```java
findByProductId(...)
findByProductIdAndType(...)
findByProductIdAndTypeAndOccurredAtBetween(...)
```

`Specification` permet d'ajouter les filtres un par un.

Exemple :

```java
if (query.productId() != null) {
    specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("productId"), query.productId().getValue()));
}
```

Traduction SQL :

```sql
where product_id = ?
```

Pour `locationId`, on utilise un `OR` :

```java
criteriaBuilder.or(
        criteriaBuilder.equal(root.get("sourceLocationId"), query.locationId().getValue()),
        criteriaBuilder.equal(root.get("destinationLocationId"), query.locationId().getValue())
)
```

Pourquoi ? Parce qu'un emplacement peut être source ou destination.

```text
ENTRY    -> destination seulement
EXIT     -> source seulement
TRANSFER -> source + destination
```

Donc filtrer par location doit trouver les mouvements où l'emplacement apparaît dans l'une
des deux colonnes.

#### 10. `toView(...)` : transformer JPA vers un DTO applicatif

La base retourne une entity :

```java
StockMovementJpaEntity
```

L'application ne veut pas exposer directement cette entity. Elle veut une vue de lecture :

```java
StockMovementView
```

La méthode `toView` traduit donc une ligne de base en objet applicatif.

Exemple `ENTRY` :

```text
sourceLocationId = null
destinationLocationId = rayon
movementType = ENTRY
```

Résultat :

```text
locationId = rayon
destinationLocationId = null
```

Exemple `EXIT` :

```text
sourceLocationId = rayon
destinationLocationId = null
movementType = EXIT
saleId = vente-123
```

Résultat :

```text
locationId = rayon
destinationLocationId = null
saleId = vente-123
```

Exemple `TRANSFER` :

```text
sourceLocationId = reserve
destinationLocationId = rayon
movementType = TRANSFER
```

Résultat :

```text
locationId = reserve
destinationLocationId = rayon
```

À retenir : `locationId` est l'emplacement principal du mouvement. Pour un transfert, on
ajoute aussi `destinationLocationId`.

#### 11. Wiring Spring : `@Configuration` + `@Bean`

Nos use cases application sont des classes Java simples :

```java
public class ListStockMovementsUseCase {
}
```

Elles n'ont pas `@Service`. Spring ne les crée donc pas automatiquement.

On ajoute une configuration :

```java
@Configuration
public class UseCaseConfiguration {
    @Bean
    public ListStockMovementsUseCase listStockMovementsUseCase(
            StockMovementQueryPort stockMovementQueryPort
    ) {
        return new ListStockMovementsUseCase(stockMovementQueryPort);
    }
}
```

Traduction humaine :

```text
Spring, si quelqu'un demande un ListStockMovementsUseCase,
voici comment tu dois le construire.
```

Le controller peut alors recevoir le use case dans son constructeur :

```java
public StockMovementController(ListStockMovementsUseCase useCase) {
    this.listStockMovementsUseCase = useCase;
}
```

#### 12. `SpringDomainEventPublisher`

L'application dépend du port :

```java
EventPublisher
```

Elle ne connaît pas Spring. L'infrastructure fournit une implémentation :

```java
public class SpringDomainEventPublisher implements EventPublisher {
    public void publish(List<DomainEvent> events) {
        events.forEach(applicationEventPublisher::publishEvent);
    }
}
```

Traduction :

```text
Le use case dit : publie mes events métier.
L'infrastructure les transmet au système d'events de Spring.
```

On garde donc l'application indépendante de Spring, tout en utilisant Spring au bord du
système.

---

### Session révision — JPA `@Query` vs `Specification`, projection constructeur, proxy Spring Data, IoC

Session de compréhension approfondie du code de lecture (`GET /stock-movements` et
`GET /stock-levels`) et du câblage des beans. Concepts transverses, pas un ticket.

#### 1. Deux routes pour le même problème : filtres optionnels + pagination

Le même besoin (filtrer dynamiquement + paginer) a été résolu de **deux façons** dans le
projet, et c'est un choix d'architecte, pas une incohérence :

```text
StockMovement (GET /stock-movements) -> Specification JPA (filtres composés en Java)
StockLevel    (GET /stock-levels)    -> @Query JPQL écrite à la main + projection
```

Critère de choix :

```text
beaucoup de filtres combinables, peu de jointures   -> Specification
jointures complexes + projection directe vers DTO   -> @Query JPQL
```

#### 2. JPQL n'est pas SQL

JPQL travaille sur les **entités Java**, pas sur les tables. Hibernate le traduit ensuite en
SQL.

```text
SQL  : SELECT * FROM stock_levels          (table)
JPQL : select sl from StockLevelJpaEntity sl   (entité)
```

Dans une `@Query`, écrire `StockLevelJpaEntity` désigne la classe Java ; Hibernate fera la
traduction vers la table `stock_levels`.

#### 3. Projection par constructeur (`select new`)

Au lieu de retourner des entités JPA complètes, la requête construit directement un DTO de
lecture :

```java
@Query("""
        select new com.aliCheikh.stock.infrastructure.persistence.projection.StockLevelRow(
            stockLevel.id.productId,
            product.name,
            stockLevel.id.locationId,
            location.label,
            location.locationType,
            location.shopId,
            stockLevel.quantity
        )
        from StockLevelJpaEntity stockLevel
        ...
        """)
Page<StockLevelRow> findByQuery(UUID productId, UUID shopId, UUID locationId, Pageable pageable);
```

Conditions strictes pour que ça compile au démarrage :

- `StockLevelRow` doit avoir un **constructeur** prenant exactement ces champs, **dans cet
  ordre**, **avec ces types** ;
- on doit donner le **nom de classe complet** (`com.aliCheikh...StockLevelRow`).

Avantage : on charge **uniquement les colonnes nécessaires** (pas l'entité entière), zéro
lazy loading, zéro N+1, et **pas de mapper manuel** entity -> view (contrairement à
`StockMovement` qui mappait dans l'adapter). C'est la bonne pratique pour un read model.

#### 4. Deux types de jointures JPQL

```java
from StockLevelJpaEntity stockLevel
join stockLevel.location location                                  -- (A) association mappée
join ProductJpaEntity product on product.id = stockLevel.id.productId  -- (B) jointure ad-hoc
```

- **(A)** `stockLevel.location` : `StockLevelJpaEntity` a un champ relation JPA (`@ManyToOne`)
  vers `LocationJpaEntity`. Hibernate connaît déjà la condition de jointure (la FK).
- **(B)** Pas de relation mappée entre `StockLevel` et `Product` (on ne garde que le
  `productId` en UUID brut, choix DDD). Donc on écrit la condition `on ...` explicitement.

#### 5. Filtres optionnels avec `IS NULL OR`

```java
where (:productId is null or stockLevel.id.productId = :productId)
  and (:shopId is null or location.shopId = :shopId)
  and (:locationId is null or stockLevel.id.locationId = :locationId)
```

Chaque ligne : *"soit le param est null (filtre ignoré), soit on filtre dessus"*. Pour
`?productId=4444&shopId=8888` (locationId absent) le WHERE effectif devient :

```sql
WHERE stock_level.product_id = '4444' AND location.shop_id = '8888'
-- la condition sur locationId est toujours vraie -> s'évapore
```

Nuance perf à connaître : `(:p is null or col = :p)` peut empêcher l'usage optimal d'un index
sur de grosses tables (condition non sargable). Négligeable pour 3 filtres ; à grande échelle,
préférer les Specifications.

#### 6. Comment `findByQuery` se branche sur la `@Query` (proxy Spring Data)

Le repository est une **interface sans corps**. Au démarrage, Spring Data **génère une classe
proxy** qui l'implémente. Quand on appelle `findByQuery(...)`, le proxy :

```text
1. lit l'annotation @Query posée sur la méthode
2. lie chaque argument au paramètre nommé correspondant de la requête
3. exécute via l'EntityManager (Hibernate)
4. construit les StockLevelRow (select new) et emballe dans un Page
```

#### 7. Binding des paramètres : par nom (grâce à `-parameters`), `Pageable` par type

```text
:productId  <- UUID productId    (lié PAR NOM)
:shopId     <- UUID shopId
:locationId <- UUID locationId
Pageable    -> pas de :pageable ; détecté PAR TYPE, pilote LIMIT/OFFSET/ORDER BY + count
```

Le binding par nom **sans `@Param`** fonctionne uniquement parce que le projet compile avec le
flag `-parameters` (qui préserve les noms réels dans le bytecode). Sans ce flag, `productId`
deviendrait `arg0` et il faudrait `@Param("productId")` explicite, sinon l'app **plante au
démarrage** ("Name for argument not specified").

#### 8. La famille pagination : 4 noms, 4 rôles

```text
Sort         -> objet de tri (ORDER BY). Sort.by(Sort.Order.desc("occurredAt"))
Pageable     -> INTERFACE "je transporte page+size+sort" (comme List)
PageRequest  -> IMPLÉMENTATION concrète de Pageable (comme ArrayList).
                PageRequest.of(page, size, sort)
Page<T>      -> RÉSULTAT de findAll : getContent(), getTotalElements(), getTotalPages()
```

`PageRequest.of(2, 10)` -> `LIMIT 10 OFFSET 20` (offset = page * size).

#### 9. `@RequestParam` séparés vs Request DTO (`@ModelAttribute`)

Pour un GET, deux styles valables :

```java
// Style A (choisi) : params séparés
listStockMovements(@RequestParam(defaultValue="0") @Min(0) int page, ...)

// Style B : Request DTO rempli depuis les query params
listStockMovements(@Valid @ModelAttribute ListStockMovementsRequest request)
```

On a gardé A pour ce projet car : sémantique HTTP (query params = switches indépendants),
`defaultValue` natif et concis, et `sort` multi-valeur nécessite un `MultiValueMap` de toute
façon. Bascule vers B justifiée si > 10 params ou validation cross-field (ex : `from < to`).

Règle : **GET = `@RequestParam`, POST = `@RequestBody` DTO**, et on s'y tient pour la cohérence.

#### 10. Le pont IoC : `UseCaseConfiguration` et les `@Bean`

Les use cases et services domaine sont des **POJO purs** (aucun `@Service`/`@Component`) pour
garder `stock-application`/`stock-domain` indépendants de Spring. Conséquence : Spring ne peut
pas les auto-détecter par component scan. On les déclare donc **manuellement** dans une classe
`@Configuration` côté infrastructure :

```java
@Configuration
public class UseCaseConfiguration {
    @Bean
    public ReceiveStockUseCase receiveStockUseCase(
            ProductRepository productRepository,          // <- port, adapter @Repository auto-détecté
            ReceivingService receivingService,            // <- @Bean défini juste au-dessus
            ...) {
        return new ReceiveStockUseCase(productRepository, receivingService, ...);
    }
}
```

Mécanique : `@Bean` = "le retour de cette méthode est un bean". Les **paramètres** de la
méthode sont injectés par Spring en cherchant un bean compatible dans le conteneur. Spring
résout le **graphe de dépendances** et instancie dans le bon ordre (ingrédients avant plats).

Deux sources de beans :

```text
auto-détectés  : adapters JPA (@Repository), publisher (@Component), controllers (@RestController)
déclarés à la main : use cases + services purs (@Bean dans UseCaseConfiguration)
```

Phrase entretien : *"Mes use cases restent des POJO sans annotation framework ; le câblage se
fait dans une @Configuration côté infrastructure. Le cœur métier reste testable sans conteneur
et migrable vers un autre framework sans le toucher."*

#### 11. Comparatif Specification vs `@Query` JPQL

```text
                              Specification        @Query JPQL
filtres dynamiques nombreux   ++ (composables)     -- (IS NULL OR lourd)
jointures complexes           -- (verbeux)         ++ (lisible comme SQL)
projection directe -> DTO     -- (mapper après)    ++ (select new)
perf à grande échelle         ++ (pas de IS NULL)  ~ (IS NULL OR moins sargable)
lisibilité humaine            ~ (lambdas)          ++ (proche SQL)
type-safety                   ~ (root.get("str"))  ~ (JPQL en string)
```

**À retenir global :** on choisit l'outil selon la forme du problème. `StockMovement`
(beaucoup de filtres, OR sur location) -> Specification. `StockLevel` (2 jointures, projection
multi-tables) -> `@Query` JPQL avec `select new`. Les deux sont défendables, et le critère de
choix doit être documenté.

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
- **`@Validated` oublié sur la classe** → annotations `@Min`/`@Max` posées sur les query
  params **silencieusement ignorées**. La validation ne s'exécute pas, aucune erreur, faux
  sentiment de sécurité.
- **`ConstraintViolationException` non handlée** → Spring renvoie 500 par défaut pour les
  violations de Bean Validation sur les query params, alors qu'on attend 400. Asymétrie avec
  `@Valid` sur `@RequestBody` qui donne 400 automatiquement.
- **Calcul de `totalPages` avec `content.size()`** au lieu de `totalElements` → résultat
  dépend de la page demandée, totalement absurde. Toujours utiliser `totalElements`.
- **Confondre `@Valid` (Jakarta) et `@Validated` (Spring)** → ils ne valident pas les
  mêmes choses. `@Valid` sur un `@RequestBody`, `@Validated` au niveau classe pour les
  query params.
- **Exposer `Page<T>` de Spring Data en JSON** → fuite de la structure interne au client,
  champs redondants, contrat couplé au framework.
- **Utiliser `now()` dans un test de controller** → date non déterministe. Préférer une date
  fixe (`LocalDateTime.of(...)`) et asserter le JSON exact.
- **Retourner `202 Accepted` par réflexe sur un POST** → lire OpenAPI. Pour `POST /sales`,
  le contrat demande `201 Created`.
- **Mapper `lineTotal` en JSON `lineTotal`** → le contrat OpenAPI attend `subtotal`.
- **Oublier `@Valid` sur `lines`** → `@Positive` sur `quantity` ne descend pas dans les
  objets imbriqués.
- **Tester `lines` vide avec un autre champ invalide** → le test passe pour la mauvaise
  raison. Garder les autres champs valides pour isoler le cas testé.
- **Oublier `-am` après avoir modifié `stock-application`** → `stock-infrastructure` peut
  compiler contre une ancienne version installée localement.
- **Utiliser `-Dtest=...` avec `-am` sans `-Dsurefire.failIfNoSpecifiedTests=false`** →
  Maven peut échouer dans un module qui ne contient pas ce test ciblé.
- **Utiliser `@RequestParam List<String> sort` pour `sort=executedAt,desc`** → Spring peut
  découper la valeur en `["executedAt", "desc"]`. Pour garder la valeur brute, utiliser
  `MultiValueMap<String, String>`.
- **Trier avec les noms de l'API directement en JPA** → `executedAt` n'existe pas dans
  l'entity, il faut traduire vers `occurredAt`.
- **Exposer `Page<T>` ou `StockMovementJpaEntity` hors persistence** → fuite de Spring Data
  ou JPA dans l'application/web. Utiliser `PageResult<T>` et `StockMovementView`.
- **Oublier le wiring `@Bean` d'un use case sans `@Service`** → Spring ne sait pas construire
  le controller en runtime.

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

### Bean Validation côté HTTP — `@Valid` vs `@Validated`

> "Pour valider les inputs HTTP, Spring offre deux mécanismes. `@Valid` (Jakarta) sur un
> `@RequestBody` valide les champs d'un objet désérialisé ; une violation lève
> `MethodArgumentNotValidException` que Spring mappe automatiquement en 400. `@Validated`
> (Spring) au niveau classe active la validation des paramètres de méthode — query params,
> path variables — quand on pose `@Min`/`@Max`/etc. dessus. **Piège** : une violation lève
> `ConstraintViolationException` que Spring renvoie en 500 par défaut. Il faut un
> `@ExceptionHandler` explicite pour ramener à 400. C'est une asymétrie historique de
> Spring qu'il faut connaître."

### Pagination — convention et calcul

> "Notre API expose une pagination page-based avec query params `page` et `size`, valeurs
> par défaut 0 et 20, bornes 0 et 200 pour size. Le port domaine utilise la même
> nomenclature pour cohérence et simplicité d'adapter. Le wrapper DTO `PageOfProductResponse`
> contient le `content` et les métadonnées (`page`, `size`, `totalElements`, `totalPages`).
> `totalPages` se calcule par `ceil(totalElements / size)`, jamais par `content.size() / size`
> qui dépendrait de la page courante. On évite d'exposer `Page<T>` de Spring Data
> directement parce que sa structure est verbeuse, redondante et couplée au framework."

### TDD discipline complète

> "Sur ce ticket de pagination, j'ai appliqué le TDD strict : six cycles Red-Green-Refactor,
> un test par scénario d'acceptation, un commit par cycle vert. Trois des six tests sont
> passés du premier coup parce que le code de production écrit pour les cycles précédents
> les couvrait déjà — autant de régression tests gagnés sans effort. Cette discipline
> garantit que chaque ligne de code de production est motivée par un test, donc couverte
> par définition, et que `main` reste toujours dans un état cohérent."

### Wrapper DTO de pagination vs `Page<T>` Spring

> "On ne sérialise jamais directement le `Page<T>` de Spring Data en JSON. Deux raisons.
> D'abord, sa structure est verbeuse et redondante — `pageable.pageSize` vs `size`,
> `number` vs `pageable.pageNumber`. Ensuite, exposer Spring Data dans le contrat HTTP
> couple le contrat au framework de persistance — si demain on change de stack, le contrat
> casse. On définit donc nos propres DTOs `PageOfProductResponse` et `PageMetaResponse`
> alignés sur le schéma OpenAPI. Le mapper fait la traduction, le contrat reste indépendant
> et propre."

### Lire OpenAPI avec `$ref`

> "Dans OpenAPI, un endpoint référence souvent des schémas avec `$ref`. Le bloc `responses`
> ne contient pas forcément les champs de la réponse ; il pointe vers
> `components.schemas.SaleResponse`. Pour implémenter un endpoint, je lis d'abord le path
> et le statut HTTP, puis je suis les `$ref` vers les schémas request/response. C'est ce qui
> permet de déduire les DTOs HTTP et de voir si le use case retourne assez d'informations."

### Use case result pour construire une réponse HTTP

> "Si un endpoint doit renvoyer un body riche, le use case ne peut pas toujours rester
> `void`. Pour `POST /sales`, la réponse doit contenir `saleId`, les lignes, le total et
> `createdAt`. Ces données appartiennent au scénario applicatif et viennent de la `Sale`
> créée. Le controller ne doit pas les rechercher lui-même dans les repositories. Le use case
> retourne donc un `SellProductResult`, que le mapper transforme en `SaleResponse`."

### Timestamp métier vs timestamp technique

> "Quand un agrégat possède déjà un timestamp métier, la réponse doit réutiliser ce
> timestamp. Pour une vente, `createdAt` vient de `sale.getOccurredAt()`, pas de
> `LocalDateTime.now()` au moment de construire le result. Sinon on mélange le moment métier
> de création et le moment technique de mapping, avec un léger décalage possible."

### `201 Created` vs `202 Accepted`

> "`201 Created` signifie que la ressource demandée a été créée immédiatement, comme une
> `Sale`. `202 Accepted` signifie que la requête est acceptée mais que le traitement ou la
> ressource de suivi n'est pas forcément exposé immédiatement. Je ne choisis pas le statut
> au feeling : je lis le contrat OpenAPI et je l'aligne avec le sens HTTP."

---

## Glossaire des termes techniques

Définitions courtes pour fixer le vocabulaire. Consultable rapidement le matin.

**Adapter** — implémentation concrète d'un port. Dans `infrastructure/`,
`ProductJpaRepositoryAdapter` implémente `ProductRepository` (port du domaine).
L'adapter "adapte" une technologie externe (JPA) à l'interface attendue par le domaine.

**Agrégat** — cluster d'objets domaine traité comme une unité de cohérence
transactionnelle. Exemple : `Sale` contient ses `SaleLineItem`. Modifications passent
par la racine (`Sale`), jamais directement sur les enfants. Concept DDD central.

**Annotation** — étiquette Java lisible à l'exécution. Spring scanne les annotations
pour décider quoi faire des classes (créer un bean, mapper une route, etc.). Voir
distinction stéréotype vs configuration.

**ApplicationContext** — registre interne de Spring où sont stockés tous les beans
créés au démarrage. Le "conteneur IoC". On peut le voir comme une grosse Map dont
les clés sont les types (et noms) des beans.

**Bean** — instance de classe gérée par Spring (créée, stockée dans
l'ApplicationContext, injectable). Différent d'une annotation.

**BOM (Bill of Materials)** — POM Maven spécial qui ne contient que des
`<dependencyManagement>`. Importé pour centraliser les versions cohérentes d'un
écosystème (ex. `spring-boot-dependencies`).

**Bytecode** — code compilé Java (fichiers `.class`) exécuté par la JVM. Pas du code
machine, c'est intermédiaire.

**CQRS (Command Query Responsibility Segregation)** — pattern qui sépare les
écritures (Command) des lectures (Query). Pragmatique chez nous : commandes via use
cases, lectures simples directement au repository.

**DDL / DML** — DDL (Data Definition Language) = SQL qui modifie le schéma : CREATE,
ALTER, DROP. DML (Data Manipulation Language) = SQL qui modifie les données : INSERT,
UPDATE, DELETE. Flyway gère le DDL, pas le DML.

**DispatcherServlet** — servlet central de Spring MVC qui reçoit toutes les requêtes
HTTP et les route vers le bon controller. Le "chef d'orchestre".

**DTO (Data Transfer Object)** — objet plat sans logique, utilisé pour transporter
des données entre couches. Chez nous : DTOs Request / Response côté HTTP. Pas à
confondre avec les Command applicatifs ou les agrégats domaine.

**Embedded server** — serveur web (Tomcat, Jetty, Undertow) inclus dans le jar
applicatif. Spring Boot embarque Tomcat par défaut. Plus besoin de déployer dans un
serveur externe.

**Factory method** — méthode statique qui crée et retourne une instance d'une classe,
souvent en remplaçant le constructeur public. Permet de nommer le mode de
construction (`Money.of(...)`, `ProductId.of(...)`).

**Flyway** — outil de migration de base de données. Versionne le schéma SQL via des
fichiers `V<n>__description.sql` exécutés dans l'ordre. Une migration appliquée n'est
plus jamais modifiée.

**Hexagonale (architecture)** — architecture où le domaine est au centre, les
adapters (web, persistance, etc.) sont en périphérie, et les dépendances vont
toujours **vers** le domaine. Aussi appelée "ports and adapters".

**Idempotency** — propriété d'une opération qui produit le même résultat si elle est
exécutée plusieurs fois. Critique pour les retries HTTP (pattern Idempotency-Key).

**Inversion de dépendance (DIP)** — principe SOLID. Une classe haute (controller) ne
dépend pas d'une classe basse (adapter JPA), mais d'une abstraction (port). Permet de
remplacer l'implémentation sans toucher au consommateur.

**JPA (Jakarta Persistence API)** — spécification Java pour le mapping objet-relationnel.
Hibernate est l'implémentation la plus utilisée. Annotations principales : `@Entity`,
`@Id`, `@Column`, `@OneToMany`, `@ManyToOne`, `@Version`.

**JSONPath** — mini-langage de requête sur du JSON. Syntaxe `$.field.subfield[0]`.
Utilisé dans MockMvc avec `jsonPath("$.status").value("UP")`.

**MockBean** — annotation Spring Test qui remplace un bean du contexte par un mock
Mockito. Essentiel dans les `@WebMvcTest` pour mocker les dépendances absentes du
slice.

**MockMvc** — outil Spring Test qui simule des requêtes HTTP en mémoire, sans ouvrir
de port. Fluent API : `mockMvc.perform(get(...)).andExpect(status().isOk())`.

**N+1 (problème)** — anti-pattern JPA : on charge une liste de N entités, puis pour
chaque entité on fait une requête supplémentaire (total N+1 requêtes). Symptôme :
lenteur soudaine en prod. Solution : `@EntityGraph`, `JOIN FETCH`.

**Optional** — type Java qui enveloppe une valeur potentiellement absente.
Préférable au retour `null` quand l'absence est attendue. API riche : `map`, `filter`,
`orElseThrow`.

**POJO (Plain Old Java Object)** — objet Java sans dépendance à un framework. Pas
d'annotations Spring/JPA/Jackson. Le domaine doit être 100 % POJO.

**Port / adapter** — port = interface définie dans le domaine (ex.
`ProductRepository`). Adapter = implémentation dans l'infrastructure
(ex. `ProductJpaRepositoryAdapter`).

**Pull Request (PR)** — proposition de merger une branche dans une autre, ouverte
sur GitHub/GitLab. Permet la revue de code avant intégration. Standard entreprise.

**Réflexion (Java Reflection API)** — capacité d'un programme Java à inspecter ses
propres classes à l'exécution : lister les méthodes, lire les champs, invoquer
dynamiquement. Utilisée par Jackson, Spring, JPA.

**Record** — type Java introduit en Java 14, déclaré en une ligne. Immutable, le
compilateur génère constructeur, accesseurs, `equals`, `hashCode`, `toString`. Idéal
pour les DTOs.

**REST (Representational State Transfer)** — style d'architecture pour les API web.
Principes : ressources identifiées par URL, verbes HTTP standards (GET, POST, PUT,
DELETE), sans état côté serveur, format de représentation négocié (JSON).

**Slice test** — test qui ne charge qu'une tranche fine du contexte Spring (ex.
`@WebMvcTest` pour la couche web, `@DataJpaTest` pour JPA). Rapide et isolé.

**Starter** — dépendance Maven agrégée fournie par Spring Boot qui tire un ensemble
cohérent de bibliothèques. Ex. `spring-boot-starter-web` = web + Jackson + Tomcat.

**Stéréotype (annotation)** — annotation qui crée un bean Spring : `@Component`,
`@Service`, `@Repository`, `@RestController`, `@Configuration`. Toutes héritent de
`@Component`.

**Testcontainers** — bibliothèque Java qui lance des containers Docker éphémères
(Postgres, Redis, Kafka, etc.) pour les tests d'intégration. Plus fidèle qu'un H2
en mémoire.

**Tomcat** — serveur web Java open source. Embarqué par défaut dans Spring Boot
(starter-tomcat). Écoute sur le port 8080 en dev.

**Use case / Command** — use case = service applicatif qui orchestre une opération
métier (`SellProductUseCase`). Command = objet d'entrée d'un use case
(`SellProductCommand`), contient les paramètres d'invocation.

**Bean Validation** — spécification Java standard (`jakarta.validation`) pour la
validation déclarative. Annotations : `@NotNull`, `@NotBlank`, `@Min`, `@Max`, `@Size`,
`@Pattern`, `@Email`, etc. Implémentation par défaut : Hibernate Validator (inclus dans
`spring-boot-starter-web`).

**`@Validated`** (Spring) — annotation au niveau classe qui **active** la validation des
paramètres de méthode (query params, path variables). Différente de `@Valid` (Jakarta) qui
s'utilise sur un `@RequestBody` pour valider un objet complet.

**`@ExceptionHandler`** — annotation Spring qui désigne une méthode comme handler d'une
exception spécifique. Posée dans un controller (handler local à ce controller) ou dans
une classe annotée `@RestControllerAdvice` (handler global à toute l'application).

**`ConstraintViolationException`** — exception Jakarta levée quand `@Validated` détecte
qu'une contrainte (`@Min`, `@Max`, etc.) est violée sur un paramètre. **Piège** : Spring la
mappe en 500 par défaut, il faut un `@ExceptionHandler` pour ramener à 400.

**`MethodArgumentNotValidException`** — exception Spring levée quand `@Valid` détecte
qu'une contrainte est violée sur un `@RequestBody`. Spring la mappe **automatiquement**
en 400. Asymétrie historique avec `ConstraintViolationException`.

**Hibernate Validator** — implémentation par défaut de Bean Validation. Inclus
automatiquement dans `spring-boot-starter-web`. Ne pas confondre avec Hibernate ORM
(la couche de persistance), aucun lien.

**`Pageable` / `Page<T>`** — interfaces Spring Data pour la pagination. `Pageable`
représente une demande de page (numéro + taille + tri). `Page<T>` représente une page
de résultat (contenu + métadonnées). À **ne pas exposer** directement dans une API
publique — créer un wrapper DTO custom.

**`PageRequest`** — implémentation concrète de `Pageable` la plus courante. Construite
via `PageRequest.of(page, size)` ou `PageRequest.of(page, size, sort)`.

**`@MockitoBean`** — annotation Spring Test (depuis Spring Boot 3.4 / Spring Framework
6.2) qui remplace un bean du contexte par un mock Mockito. Standard moderne. Remplace
`@MockBean` (déprécié, sera supprimé). Import :
`org.springframework.test.context.bean.override.mockito.MockitoBean`.

**Hunk** (Git) — bloc de modifications contiguës dans un diff. Git divise les changements
d'un fichier en hunks pour les afficher et permettre le staging granulaire via
`git add -p`.

---

## Patterns Java idiomatiques rencontrés

**Record** — déclaration concise d'un type immutable. Une ligne au lieu de 30. Le
compilateur génère tout. À utiliser pour les DTOs, value objects, Commands.

**Compact constructor** — bloc d'init qui s'exécute à chaque construction d'un record.
Permet d'ajouter des guards (`Objects.requireNonNull`, `if/throw`). À utiliser sur les
Commands et value objects, pas sur les DTOs Response.

**Optional** — retour qui exprime l'absence possible. À utiliser en retour, jamais
comme paramètre ni comme champ (recommandation Effective Java item 55).

**Stream / map / filter** — transformation fonctionnelle de collections. Préférer aux
boucles `for` quand on transforme ou filtre. Exemple :

```java
List<ProductResponse> responses = products.stream()
        .map(ProductWebMapper::toResponse)
        .toList();
```

**Utility class** — classe qui n'expose que des méthodes statiques. Pattern :
- classe `final` (interdit l'extension)
- constructeur `private` (interdit l'instanciation)
- toutes les méthodes `public static`

**Static import** — importer une méthode statique pour l'utiliser sans préfixer le
nom de classe. Utile pour les DSL fluent (`get(...)`, `status()`, `assertThat(...)`).

**Method reference** — raccourci pour les lambdas. `ProductWebMapper::toResponse` est
équivalent à `product -> ProductWebMapper.toResponse(product)`. Plus lisible.

---

## Self-quiz quotidien

Questions à se poser sans regarder les réponses. Si on bute, on relit la section
correspondante. Objectif : réussir à toutes répondre en moins de 10 minutes.

1. Quelles sont les étapes du cycle d'une requête HTTP dans Spring (de Tomcat au JSON
   de réponse) ?
2. Quelle est la différence entre `@Component` et `@RequestMapping` en termes de
   création de bean ?
3. Pourquoi un `@RestController` oublié donne-t-il un 404 silencieux plutôt qu'une
   erreur au démarrage ?
4. Donner les 3 raisons de préférer `Optional<T>` à un retour `T` ou `null`.
5. Donner les 3 raisons d'utiliser un DTO HTTP plutôt que de retourner l'objet métier.
6. Pourquoi un montant monétaire doit-il être transporté en string et pas en number
   dans un JSON ?
7. Différence concrète entre `@WebMvcTest` et `@SpringBootTest` : ce qui est chargé,
   la vitesse, l'usage.
8. Donner les 3 raisons d'utiliser l'injection par constructeur plutôt que par champ.
9. Pourquoi le flag `-parameters` du compilateur Maven est-il nécessaire avec Spring,
   et comment se passe le binding sans lui ?
10. Pourquoi, dans l'archi hexagonale, le sens des dépendances est-il toujours
    `infrastructure → domaine` ?
11. Quand utilise-t-on `ResponseEntity<T>` plutôt qu'un retour direct depuis un
    controller ?
12. Différence entre `git commit --amend` et `git rebase -i HEAD~N`.
13. Pourquoi `mvn spring-boot:run` à la racine du projet échoue-t-il sur un projet
    multi-module ?
14. Pourquoi ne jamais rebase un commit déjà pushé sur une branche partagée ?
15. Donner 3 exemples de Conventional Commit conformes (type + scope + description).
16. Comment Jackson sérialise-t-il un record sans aucune annotation ? Quelle
    mécanique Java permet cela ?
17. Quelle annotation pose-t-on sur le repository pour qu'il soit mocké automatiquement
    dans un `@WebMvcTest`, et pourquoi est-elle nécessaire ?
18. Pourquoi ne met-on pas de données de seed dans Flyway ?
19. Que fait `mvn -pl stock-infrastructure -am spring-boot:run` (décompose `-pl` et
    `-am`) ?
20. Pourquoi utilise-t-on `BigDecimal.toPlainString()` et pas `BigDecimal.toString()`
    pour sérialiser un montant ?
21. Quelle différence entre `@Valid` (Jakarta) et `@Validated` (Spring) ? Quand utiliser
    lequel ?
22. Pourquoi `ConstraintViolationException` est-elle renvoyée en 500 par défaut alors que
    `MethodArgumentNotValidException` est renvoyée en 400 automatiquement ? Comment
    corriger ?
23. Que se passe-t-il si on pose `@Min(0)` sur un `@RequestParam` mais qu'on oublie
    `@Validated` au niveau classe ?
24. Donner la formule exacte de `totalPages` à partir de `totalElements` et `size`. Quel
    piège classique faut-il éviter ?
25. Pourquoi `count()` retourne-t-il `long` et pas `int` ?
26. Pourquoi ne pas exposer directement `Page<T>` de Spring Data en JSON dans une API
    publique ? Donner deux raisons.
27. Différence entre `@PathVariable` et `@RequestParam` : où chacun lit-il sa valeur ?
28. À quoi sert `defaultValue` sur `@RequestParam` ? Pourquoi est-il toujours une string,
    même pour un `int` ?
29. Cycle TDD : Red → Green → Refactor → ?
30. À quoi sert `git add -p` et dans quelle situation typique l'utilise-t-on ?
31. Pourquoi a-t-on choisi `page/size` plutôt que `offset/limit` pour la signature du port
    domaine ? Quelle est l'alternative défendable ?
32. À quoi sert `$ref: '#/components/schemas/SaleResponse'` dans OpenAPI ?
33. Pourquoi `SellProductUseCase` ne peut plus retourner `void` pour `POST /sales` ?
34. Pourquoi `SellProductResult.createdAt` doit venir de `Sale.getOccurredAt()` et pas
    de `LocalDateTime.now()` ?
35. Pourquoi `POST /sales` retourne `201 Created` alors que `POST /stock-receipts`
    retournait `202 Accepted` ?
36. Pourquoi le DTO HTTP expose `subtotal` alors que le domaine utilise `lineTotal` ?
37. Quelle différence entre `@NotNull` et `@NotEmpty` sur `lines` ?
38. Pourquoi `@Positive` sur `CreateSaleLine.quantity` nécessite aussi `@Valid` sur
    la liste `lines` ?
39. Pourquoi un test multi-lignes est utile même si le mapper stream passait déjà ?
40. Pourquoi `LocalDateTime.toString()` peut différer de la sérialisation JSON Jackson ?
41. Pourquoi `TransferStockUseCase` doit retourner un `TransferStockResult` ?
42. Pourquoi `POST /stock-transfers` retourne `202 Accepted` et seulement un acknowledgement ?
43. Pourquoi `verifyNoInteractions` est important dans les tests de validation web ?
44. À quoi servent `-am` et `-Dsurefire.failIfNoSpecifiedTests=false` dans Maven multi-module ?
45. À quoi sert un query use case comme `ListStockMovementsUseCase` ?
46. Pourquoi `ListStockMovementsQuery` regroupe-t-il les filtres au lieu de passer 8 paramètres ?
47. Pourquoi utilise-t-on `PageResult<T>` au lieu de `Page<T>` de Spring Data dans l'application ?
48. Qu'est-ce qu'un query port ? Pourquoi `StockMovementQueryPort` ne connaît-il pas JPA ?
49. Pourquoi `@RequestParam MultiValueMap<String, String>` est utile pour le paramètre `sort` ?
50. Comment traduire `sort=executedAt,desc` en `Sort.Order.desc("occurredAt")` ?
51. À quoi sert une `Specification` JPA ? Pourquoi est-elle utile avec des filtres optionnels ?
52. Pourquoi le filtre `locationId` cherche-t-il dans `sourceLocationId` OU `destinationLocationId` ?
53. Pourquoi `toView(...)` choisit-il `sourceLocationId` sinon `destinationLocationId` pour remplir `locationId` ?
54. Pourquoi faut-il déclarer des `@Bean` pour des use cases qui n'ont pas `@Service` ?
55. Quel rôle joue `SpringDomainEventPublisher` entre l'application et Spring ?
56. Quelle est la différence entre JPQL et SQL : sur quoi travaille chacun ?
57. Que doit obligatoirement posséder la classe cible d'une projection `select new ...(...)` ?
58. Quel avantage a la projection constructeur sur le retour d'entités JPA complètes ?
59. Différence entre une jointure JPQL via association mappée et une jointure ad-hoc (`on ...`) ?
60. Que fait le pattern `(:param is null or col = :param)` et quel est son coût perf potentiel ?
61. Comment une méthode de repository (interface sans corps) peut-elle exécuter du code ?
62. Comment les paramètres se lient-ils à une `@Query` ? Et le cas particulier de `Pageable` ?
63. Distinguer `Sort`, `Pageable`, `PageRequest` et `Page<T>`.
64. Quand basculer de `@RequestParam` séparés vers un Request DTO en GET ?
65. Pourquoi câbler les use cases via `@Bean` dans une `@Configuration` plutôt que `@Service` ?
66. Specification vs `@Query` JPQL : quel critère de choix ?

> Réponses détaillées : voir la section **« Flashcards corrigées »** (Q1–Q55 et S1–S11).

---

## Flashcards corrigées (réponses du self-quiz + session)

Mode d'emploi : lire la question, formuler la réponse à voix haute, puis vérifier. Réviser en
boucle espacée (J+1, J+3, J+7, J+15). Les numéros correspondent au "Self-quiz quotidien".

**Q1. Étapes du cycle d'une requête HTTP dans Spring (de Tomcat au JSON) ?**
R. Tomcat reçoit → `DispatcherServlet` → `HandlerMapping` trouve le controller → `HandlerAdapter`
invoque la méthode → résolution + validation des arguments (`@RequestParam`/`@RequestBody`/
`@PathVariable`) → exécution → `HttpMessageConverter` (Jackson) sérialise le retour en JSON → réponse.

**Q2. Différence entre `@Component` et `@RequestMapping` côté création de bean ?**
R. `@Component` (et `@Service`/`@Repository`/`@RestController`) **crée un bean**. `@RequestMapping`
ne crée **pas** de bean : c'est un mapping URL → méthode, posé sur un bean controller existant.

**Q3. Pourquoi un `@RestController` oublié donne un 404 silencieux ?**
R. Sans l'annotation, la classe n'est pas un bean web : aucun mapping n'est enregistré pour ses
méthodes. L'URL n'existe donc pas (404). Rien n'oblige Spring à connaître la classe → pas d'erreur au boot.

**Q4. 3 raisons de préférer `Optional<T>` à `T`/`null` ?**
R. (1) Rend l'absence explicite dans la signature. (2) Force le caller à traiter le cas vide
(`map`/`orElseGet`). (3) Évite les NPE dispersés.

**Q5. 3 raisons d'utiliser un DTO HTTP plutôt que l'objet métier ?**
R. (1) Découple le contrat public du modèle interne. (2) Évite de fuiter des champs internes/sensibles.
(3) Évite le lazy loading JPA en plein rendu + maîtrise la forme JSON.

**Q6. Pourquoi un montant en string et pas en number JSON ?**
R. Un `number` JSON = double IEEE754 → perte de précision décimale (`0.1 + 0.2`). La string préserve la
précision exacte du `BigDecimal`.
```json
{ "amount": "45.90", "currency": "EUR" }
```

**Q7. `@WebMvcTest` vs `@SpringBootTest` ?**
R. `@WebMvcTest` : slice web (controllers + Jackson + validation), reste mocké, rapide.
`@SpringBootTest` : contexte complet (DB, tous les beans), lent, pour l'intégration.

**Q8. 3 raisons de l'injection par constructeur ?**
R. (1) Dépendances `final`/immuables. (2) Objet toujours valide (impossible d'oublier une dépendance).
(3) Testable sans Spring (`new` + mocks). Bonus : détecte les cycles au démarrage.

**Q9. Pourquoi le flag `-parameters`, et le binding sans lui ?**
R. Il préserve les noms de paramètres dans le bytecode. Sans lui ils deviennent `arg0`/`arg1` et Spring
ne peut pas matcher par nom → il faut `@RequestParam("x")` / `@Param("x")` explicites, sinon erreur au boot.

**Q10. Pourquoi le sens des dépendances est-il toujours `infrastructure → domaine` ?**
R. Le métier (stable, testable) ne doit dépendre d'aucun détail (DB, web, qui changent souvent). Les
détails dépendent du métier via des ports (interfaces). C'est le DIP.

**Q11. Quand utiliser `ResponseEntity<T>` plutôt qu'un retour direct ?**
R. Quand on contrôle le status (201/202/404), ajoute des headers (`Location`), ou retourne des bodies
conditionnels. Retour direct = 200 implicite, suffisant pour les cas simples.

**Q12. `git commit --amend` vs `git rebase -i HEAD~N` ?**
R. `--amend` modifie le **dernier** commit. `rebase -i HEAD~N` réécrit les **N derniers** (réordonner,
squash, éditer, supprimer).

**Q13. Pourquoi `mvn spring-boot:run` échoue à la racine multi-module ?**
R. Le POM racine est un POM parent (`packaging pom`), sans classe `main` ni plugin exécutable. Il faut
cibler le module applicatif :
```bash
mvn -pl stock-infrastructure spring-boot:run
```

**Q14. Pourquoi ne jamais rebase un commit déjà pushé sur une branche partagée ?**
R. Le rebase réécrit l'historique (nouveaux SHA). Ceux qui ont basé leur travail sur les anciens commits
divergent → conflits et historique cassé pour tous.

**Q15. 3 exemples de Conventional Commit conformes ?**
R.
```text
feat(web): add POST /stock-receipts controller
test(application): cover ReceiveStockResult return value
fix(domain): correct Money hashCode precision
```

**Q16. Comment Jackson sérialise un record sans annotation ?**
R. Jackson lit par introspection les **composants du record** (accesseurs `productId()`, etc.) et génère
les champs JSON correspondants. Pas besoin de getters `getX()`.

**Q17. Quelle annotation pour mocker le repo dans un `@WebMvcTest`, et pourquoi ?**
R. `@MockitoBean` (ex-`@MockBean`, déprécié en SB 3.4). Nécessaire car le slice web ne charge pas les
beans non-web : il faut fournir un mock du use case/repo attendu par le controller.

**Q18. Pourquoi pas de données de seed dans Flyway ?**
R. Flyway versionne le **schéma** (structure), immuable une fois appliqué. Les données de seed varient par
environnement et ne doivent pas être figées dans une migration jouée partout.

**Q19. Que fait `mvn -pl stock-infrastructure -am spring-boot:run` ?**
R. `-pl` = ne traiter que ce module (*project list*). `-am` = construire aussi les modules dont il dépend
(*also make*, ex. `stock-application`/`stock-domain`). Donc build des deps + run du module infra.

**Q20. `BigDecimal.toPlainString()` vs `toString()` ?**
R. `toString()` peut produire de la notation scientifique (`1E+2`). `toPlainString()` force la notation
décimale simple (`100`), conforme au contrat JSON.

**Q21. `@Valid` (Jakarta) vs `@Validated` (Spring) ?**
R. `@Valid` : standard Jakarta, valide en cascade (`@RequestBody`, champs imbriqués). `@Validated` :
annotation Spring, active la validation des `@RequestParam`/`@PathVariable` (au niveau classe) et les groupes.

**Q22. Pourquoi `ConstraintViolationException` → 500 alors que `MethodArgumentNotValidException` → 400 ? Corriger ?**
R. `@Valid` sur `@RequestBody` lève `MethodArgumentNotValidException`, mappée 400 par défaut. `@Validated`
sur `@RequestParam` lève `ConstraintViolationException`, non mappée → 500. Correction :
```java
@ExceptionHandler(ConstraintViolationException.class)
public ResponseEntity<Void> handle(ConstraintViolationException e) {
    return ResponseEntity.badRequest().build();
}
```

**Q23. `@Min(0)` sur un `@RequestParam` sans `@Validated` au niveau classe ?**
R. L'annotation est **ignorée** : sans `@Validated`, Spring ne valide pas les `@RequestParam`, la valeur
invalide passe.

**Q24. Formule de `totalPages` + piège ?**
R. `totalPages = ceil(totalElements / size)`. Piège : ne **pas** calculer depuis `content.size()` (taille
de la page courante) mais depuis `totalElements`.
```java
int totalPages = (int) Math.ceil((double) totalElements / size);
```

**Q25. Pourquoi `count()` retourne `long` et pas `int` ?**
R. Le total peut dépasser `Integer.MAX_VALUE` (~2,1 milliards). `long` couvre les grandes tables.

**Q26. Pourquoi ne pas exposer `Page<T>` Spring Data en JSON ? (2 raisons)**
R. (1) Couple le contrat HTTP au framework (une montée de version Spring peut changer le JSON). (2)
Sérialise des champs internes non maîtrisés (`pageable`, `sort`) → contrat instable. → wrapper DTO custom.

**Q27. `@PathVariable` vs `@RequestParam` : où lit chacun ?**
R. `@PathVariable` lit dans le **chemin** (`/products/{id}`). `@RequestParam` lit dans la **query string**
(`?page=0`) ou le form.

**Q28. À quoi sert `defaultValue` sur `@RequestParam` ? Pourquoi toujours une string ?**
R. Fournit une valeur si le param est absent. Toujours string car la query HTTP est du texte ; Spring
convertit ensuite vers le type cible (`int`, etc.).
```java
@RequestParam(defaultValue = "0") @Min(0) int page
```

**Q29. Cycle TDD : Red → Green → Refactor → ?**
R. → **Commit** (atomique, sur cycle vert), puis on recommence un Red pour le scénario suivant.

**Q30. À quoi sert `git add -p` et quand l'utiliser ?**
R. Stage interactif **hunk par hunk**. Utile pour découper un working tree en commits atomiques quand on a
mélangé plusieurs changements.

**Q31. Pourquoi `page/size` plutôt que `offset/limit` pour le port domaine ? Alternative ?**
R. `page/size` est proche du langage REST/utilisateur, l'adapter convertit trivialement (`offset = page*size`).
Alternative défendable : `offset/limit` (proche SQL), ou keyset/curseur pour la perf sur gros volumes.

**Q32. À quoi sert `$ref: '#/components/schemas/SaleResponse'` dans OpenAPI ?**
R. Référence vers un schéma réutilisable défini ailleurs dans le fichier. Évite la duplication ; un seul
endroit à maintenir.

**Q33. Pourquoi `SellProductUseCase` ne peut plus retourner `void` pour `POST /sales` ?**
R. Le controller a besoin de `saleId`, `lines`, `totalAmount`, `createdAt` pour la réponse 201. Il ne doit
pas les rechercher en repository → le use case retourne un `SellProductResult`.

**Q34. Pourquoi `SellProductResult.createdAt` doit venir de `Sale.getOccurredAt()` et pas de `now()` ?**
R. La réponse doit refléter le timestamp **métier** de la vente créée, pas un second timestamp technique
généré au moment de construire le result.
```java
result.createdAt() == savedSale.getOccurredAt()  // pas LocalDateTime.now()
```

**Q35. Pourquoi `POST /sales` → 201 alors que `POST /stock-receipts` → 202 ?**
R. `201 Created` : une ressource (`Sale`) est créée immédiatement et consultable (+ `Location`). `202
Accepted` : requête acceptée sans ressource directement exposée/consultable.

**Q36. Pourquoi le DTO HTTP expose `subtotal` alors que le domaine utilise `lineTotal` ?**
R. Le DTO suit le vocabulaire **OpenAPI** (`subtotal`), pas le vocabulaire interne (`lineTotal`). Le mapper
absorbe la différence — c'est exactement son rôle.

**Q37. `@NotNull` vs `@NotEmpty` sur `lines` ?**
R. `@NotNull` : la liste n'est pas `null` (mais peut être vide). `@NotEmpty` : pas `null` **et** au moins
un élément.

**Q38. Pourquoi `@Positive` sur `quantity` nécessite aussi `@Valid` sur `lines` ?**
R. Sans `@Valid` sur la liste, la validation ne **descend pas** dans chaque élément. `@Valid` déclenche la
cascade vers les `@Positive` de chaque `CreateSaleLine`.
```java
@NotEmpty @Valid List<CreateSaleLine> lines
```

**Q39. Pourquoi un test multi-lignes est utile même si le mapper stream passait déjà ?**
R. Test de **non-régression** : protège contre un futur changement où le mapper ne traiterait que la
première ligne.

**Q40. Pourquoi `LocalDateTime.toString()` peut différer de la sérialisation JSON Jackson ?**
R. `toString()` omet les secondes si elles valent 0 (`2026-05-15T10:30`) ; Jackson les écrit (`...:00`).
Fixer la date dans les tests et asserter le JSON réel produit.

**Q41. Pourquoi `TransferStockUseCase` doit retourner un `TransferStockResult` ?**
R. Le controller doit renvoyer le `movementId` créé + `acceptedAt` pour la 202, sans aller le rechercher
en repository.
```java
public record TransferStockResult(MovementId movementId, Instant acceptedAt) {}
```

**Q42. Pourquoi `POST /stock-transfers` → 202 + simple acknowledgement ?**
R. Pas de ressource transfert consultable (pas de `GET /stock-transfers/{id}`). On accuse réception
(`movementId`, `acceptedAt`).

**Q43. Pourquoi `verifyNoInteractions` est important dans les tests de validation web ?**
R. Prouve qu'une requête invalide est bloquée dans la couche web et n'atteint **jamais** le use case → la
frontière de validation est respectée.
```java
verifyNoInteractions(transferStockUseCase);
```

**Q44. À quoi servent `-am` et `-Dsurefire.failIfNoSpecifiedTests=false` ?**
R. `-am` construit aussi les modules dépendants. `failIfNoSpecifiedTests=false` empêche Surefire d'échouer
dans les modules qui n'ont pas le test ciblé par `-Dtest=...`.

**Q45. À quoi sert un query use case comme `ListStockMovementsUseCase` ?**
R. Frontière claire de lecture (controller → use case → port), point d'extension (cache, autorisation,
audit), testable sans Spring. Évite que le controller construise des requêtes complexes.

**Q46. Pourquoi `ListStockMovementsQuery` regroupe les filtres au lieu de 8 paramètres ?**
R. Évite une signature à 8 args fragile ; ordre figé par les noms ; ajouter un filtre = un champ, sans
casser la signature.

**Q47. Pourquoi `PageResult<T>` au lieu de `Page<T>` Spring Data dans l'application ?**
R. `PageResult` est neutre (zéro dépendance framework). Utiliser `Page<T>` collerait `stock-application` à
Spring Data.

**Q48. Qu'est-ce qu'un query port ? Pourquoi `StockMovementQueryPort` ne connaît pas JPA ?**
R. Une interface définie côté application exprimant le besoin de lecture. L'implémentation JPA vit en infra
(DIP : l'app dépend de l'abstraction, pas du détail).

**Q49. Pourquoi `@RequestParam MultiValueMap` est utile pour `sort` ?**
R. `sort` peut apparaître plusieurs fois ; la map donne les valeurs **brutes** sans split sur la virgule,
contrairement à `@RequestParam List<String>` qui découperait `executedAt,desc`.

**Q50. Comment traduire `sort=executedAt,desc` en `Sort.Order.desc("occurredAt")` ?**
R. Split sur `,` → `["executedAt","desc"]` ; `toJpaProperty("executedAt")` → `"occurredAt"` ; direction
`desc` → `new Sort.Order(DESC, "occurredAt")`.

**Q51. À quoi sert une `Specification` JPA ? Pourquoi utile avec des filtres optionnels ?**
R. Un morceau de `WHERE` composable avec `.and()`. On n'ajoute une brique que si le filtre est présent →
`WHERE` dynamique sans 2ⁿ méthodes ni gros `IS NULL OR`.
```java
if (q.type() != null)
    spec = spec.and((root, cq, cb) -> cb.equal(root.get("movementType"), q.type()));
```

**Q52. Pourquoi le filtre `locationId` cherche `sourceLocationId` OU `destinationLocationId` ?**
R. Un mouvement implique une location comme source (ENTRY/EXIT) ou source/destination (TRANSFER). Le `or`
capture "tous les mouvements impliquant cette location".

**Q53. Pourquoi `toView` choisit `sourceLocationId` sinon `destinationLocationId` pour `locationId` ?**
R. Le contrat expose `locationId` (lieu principal) + `destinationLocationId` (TRANSFER seulement). En base
on a source+destination ; pour le `locationId` principal on prend la source si présente, sinon la destination.

**Q54. Pourquoi déclarer des `@Bean` pour des use cases sans `@Service` ?**
R. Les use cases sont des POJO purs (pas d'annotation Spring, pour garder l'application framework-agnostique).
Spring ne peut pas les auto-scanner → on les déclare dans une `@Configuration`.

**Q55. Rôle de `SpringDomainEventPublisher` entre l'application et Spring ?**
R. Adapter qui implémente le port `EventPublisher` (défini en application) via le mécanisme Spring
(`ApplicationEventPublisher`). L'application publie via l'interface ; l'infra fait le pont.

---

### Flashcards de la session — `@Query` JPQL, projection, proxy, IoC

**S1. JPQL vs SQL — sur quoi travaille chacun ?**
R. SQL travaille sur les **tables**, JPQL sur les **entités Java** (Hibernate traduit ensuite en SQL).
```text
SQL  : SELECT * FROM stock_levels
JPQL : select sl from StockLevelJpaEntity sl
```

**S2. Qu'exige une projection `select new ...(...)` pour fonctionner au démarrage ?**
R. La classe cible doit avoir un **constructeur** avec exactement ces champs, **dans cet ordre et ces
types** ; et il faut le **nom de classe complet** dans le `select new`.

**S3. Avantage de la projection constructeur sur le retour d'entités ?**
R. On ne charge que les colonnes nécessaires (pas l'entité entière), zéro lazy loading, zéro N+1, et
**pas de mapper manuel** entity → view.

**S4. Les deux types de jointures JPQL ?**
R. (A) association mappée : `join stockLevel.location location` (la relation `@ManyToOne` existe, Hibernate
connaît la FK). (B) ad-hoc : `join ProductJpaEntity p on p.id = stockLevel.id.productId` (pas de relation
mappée, on écrit la condition).

**S5. Que fait `(:productId is null or col = :productId)` ?**
R. Filtre optionnel : si le param est `null`, la condition est toujours vraie (filtre ignoré) ; sinon on
filtre. Nuance perf : peut empêcher l'usage optimal d'un index à grande échelle.

**S6. Comment `findByQuery` (interface sans corps) exécute-t-elle du code ?**
R. Spring Data **génère une classe proxy** au démarrage qui implémente l'interface, lit la `@Query`, lie
les paramètres, exécute via l'`EntityManager` et emballe le résultat dans un `Page`.

**S7. Comment les paramètres se lient-ils à la requête ? Le cas de `Pageable` ?**
R. Les UUID sont liés **par nom** (`:productId` ↔ `productId`), possible sans `@Param` grâce au flag
`-parameters`. `Pageable` n'a pas de `:pageable` : il est détecté **par type** et pilote `LIMIT/OFFSET/ORDER BY` + count.

**S8. Distingue `Sort`, `Pageable`, `PageRequest`, `Page<T>`.**
R. `Sort` = tri (ORDER BY). `Pageable` = **interface** "page+size+sort". `PageRequest` = **implémentation**
concrète (`PageRequest.of(...)`). `Page<T>` = **résultat** (`getContent`, `getTotalElements`, `getTotalPages`).
```java
PageRequest.of(2, 10) // -> LIMIT 10 OFFSET 20
```

**S9. `@RequestParam` séparés vs Request DTO en GET — quand basculer ?**
R. `@RequestParam` : peu de params, `defaultValue` natif, `sort` multi-valeur. Bascule vers un
`@ModelAttribute` Request DTO si > 10 params ou validation cross-field (`from < to`). Règle projet :
GET = `@RequestParam`, POST = `@RequestBody` DTO.

**S10. Pourquoi un fichier `UseCaseConfiguration` avec des `@Bean` ?**
R. Les use cases/services purs n'ont pas d'annotation Spring → non auto-scannés. On les câble à la main
dans une `@Configuration` (côté infra). `@Bean` = "le retour est un bean" ; ses paramètres sont injectés
depuis le conteneur ; Spring résout le graphe de dépendances.

**S11. Specification vs `@Query` JPQL — critère de choix ?**
R. Specification : beaucoup de filtres combinables, peu de jointures. `@Query` JPQL : jointures complexes +
projection directe vers DTO (`select new`). On choisit selon la forme du problème, et on documente le critère.

---

## Ressources externes recommandées

À explorer progressivement, pas à dévorer d'un coup.

**Documentation officielle (référence)**

- *Spring Boot Reference* — `docs.spring.io/spring-boot/docs/current/reference/html/`
  Section "Web" pour les controllers, "Data" pour JPA, "Testing" pour les slice tests.
- *PostgreSQL Documentation* — `postgresql.org/docs/current/` — la plus complète au
  monde sur un SGBD, parfaite pour creuser les types, contraintes, EXPLAIN.
- *Docker Documentation* — `docs.docker.com/` — pour Compose, multi-stage,
  health checks.

**Livres (à lire dans l'ordre, pas tous d'un coup)**

- *Effective Java* (Joshua Bloch) — la bible Java moderne. Items 55 (Optional), 17
  (immutabilité), 50 (defensive copies) sont à connaître par cœur.
- *Refactoring* (Martin Fowler) — pour développer le sens du code review et de
  l'amélioration continue.
- *Implementing Domain-Driven Design* (Vaughn Vernon) — pour creuser DDD. Long, dense,
  à lire après stabilisation du projet.
- *The Pragmatic Programmer* (Hunt & Thomas) — pour le mindset général de dev senior.

**Sites / référentiels**

- *Conventional Commits* — `conventionalcommits.org` — spec courte, à lire 1 fois.
- *Keep a Changelog* — `keepachangelog.com` — format de CHANGELOG, complément naturel
  de Conventional Commits.
- *Twelve-Factor App* — `12factor.net` — bonnes pratiques de config et déploiement.

**Chaînes YouTube de qualité (en anglais)**

- *Java Brains* (Koushik Kothagal) — Spring Boot pédagogique.
- *Marco Codes* — astuces Java/Spring courtes et claires.
- *Hussein Nasser* — bases de données, perf, architecture serveur.

---

## À anticiper pour les prochains tickets

Concepts qu'on va rencontrer bientôt — survol rapide pour ne pas être surpris.

**Ticket 4.3 et au-delà — Bean Validation**

Annotations `@Valid`, `@NotNull`, `@NotBlank`, `@Positive`, `@Size`, `@Min`, `@Max`
posées sur les champs d'un DTO Request. Spring les vérifie automatiquement à la
désérialisation. Violation → 400 Bad Request avec détail des champs en erreur.

**Chantier 5 — ProblemDetail RFC 7807**

Standard pour le format des erreurs HTTP. Spring Boot 3 a une classe `ProblemDetail`
qui matche le RFC. On la retournera depuis un `@RestControllerAdvice` global qui
attrape les exceptions domaine et les transforme en réponses HTTP propres.

**Chantier 5 — `@RestControllerAdvice` + `@ExceptionHandler`**

Mécanisme Spring pour centraliser la gestion des exceptions. Une seule classe
annotée `@RestControllerAdvice` peut intercepter toutes les exceptions levées par les
controllers et les mapper en réponses HTTP standardisées.

**Pagination Spring**

Pour les endpoints de liste (`GET /products`, `GET /stock-movements`), Spring fournit
`Pageable` et `Page<T>`. Paramètres URL `?page=0&size=20&sort=createdAt,desc`. On
créera un wrapper `PageOfProduct` pour ne pas exposer la structure `Page<T>` de Spring
directement (qui n'est pas idéale en JSON public).

**Chantier 6 — Spring Security + JWT**

Filter chain Spring Security, génération/validation de JWT, endpoints `/auth/login`,
`/auth/refresh`, `/auth/logout`. Token stockage côté front en cookie httpOnly (jamais
localStorage à cause du XSS).

**Chantier 7 — Idempotency-Key**

Filter qui intercepte les POST de mutation. Table `idempotency_key` avec TTL 24h.
Permet aux clients de retry sans risquer de doubler une vente.

**Chantier 8 — Domain events**

`ApplicationEventPublisher` Spring (synchrone, in-process, transactionnel). Annotation
`@TransactionalEventListener(phase = AFTER_COMMIT)` pour ne publier qu'après commit.

**Chantier 9 — Spring Actuator**

Endpoints `/actuator/health`, `/actuator/info`, `/actuator/metrics`. Distinct de
notre `/api/v1/health` applicatif : Actuator est pour l'ops (Kubernetes, Docker,
Prometheus).

**Chantier 10 — Testcontainers**

Lance une vraie Postgres jetable en Docker pendant les tests d'intégration. Plus
fidèle qu'un H2 in-memory. Annotation `@Testcontainers` + `@DynamicPropertySource`
pour brancher Spring sur le container.

---

_Fichier à enrichir à chaque ticket terminé. Relire chaque matin 10 minutes, reformuler à voix haute._
