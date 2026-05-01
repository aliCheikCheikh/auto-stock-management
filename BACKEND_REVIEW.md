# Revue du backend — auto-stock-management

**Auteur de la revue :** Claude (assistant IA)
**Date :** 23 avril 2026
**Périmètre :** `stock-domain/` + `stock-application/` (infra volontairement écarté, vide)
**Contexte :** projet Master ALMA — gestion de stock pièces auto, architecture hexagonale + DDD + TDD.

## TL;DR

Le code a une **très bonne ossature** : DDD rigoureux, invariants protégés dans les agrégats, ports bien définis, immuabilité des value objects, events de domaine pullés proprement depuis l'agrégat. La logique métier des trois use cases (SellProduct, ReceiveStock, TransferStock) est correcte dans l'intention.

**Mais il y a 6 bugs réels** (violations du contrat `equals/hashCode`, mauvaise abstraction de repository) qui vont te mordre dès que tu brancheras l'infra, et une grosse dizaine de points d'architecture à resserrer avant que le code soit défendable en soutenance ou en revue de code en entreprise.

Je classe en trois niveaux :

- **CRITIQUE** — ça cause ou causera un bug fonctionnel. À traiter avant tout.
- **IMPORTANT** — ça fragilise l'architecture, la perf ou la maintenabilité. À traiter avant le controller REST.
- **NICE-TO-HAVE** — cosmétique, cohérence, dette technique de surface.

---

## 🔴 CRITIQUE — 6 points

### C1. `LocationId` n'a pas de `hashCode()`

**Fichier :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/model/stock/LocationId.java`

Seul `equals()` est redéfini. `hashCode()` reste celui d'`Object` (identité mémoire) alors qu'`equals()` compare la valeur UUID. **Violation directe du contrat `equals/hashCode`.**

**Pourquoi c'est grave ici spécifiquement :** `LocationId` est utilisé comme **clé de Map** dans l'événement `StockReceived.locationBreakdown` (`Map<LocationId, Integer>`). Deux `LocationId` portant la même UUID auront deux hashCodes différents, donc la Map va créer deux entrées distinctes au lieu d'une. Silencieusement. Ton agrégation est fausse.

**Fix :**

```java
@Override
public int hashCode() {
    return value.hashCode();
}
```

---

### C2. `MovementId` n'a pas d'`equals()`

**Fichier :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/model/movement/MovementId.java`

Symétrique du précédent : `hashCode()` est redéfini mais pas `equals()`. Deux `MovementId` avec la même UUID produiront le même hash mais ne seront **pas** égaux (reference equality).

Impact concret : tu peux chercher un mouvement dans un `Set<MovementId>` avec un id reconstruit depuis la DB, `contains()` retournera `false`.

**Fix :**

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MovementId that = (MovementId) o;
    return value.equals(that.value);
}
```

---

### C3. `ShopId` n'a pas de `hashCode()`

**Fichier :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/model/shop/ShopId.java`

Même problème que C1. Pas exploité actuellement comme clé de Map, mais le jour où tu regroupes par shop (`Map<ShopId, List<StorageLocation>>`, typique pour le multi-boutiques que tu as annoncé), bug silencieux.

**Fix :** ajouter `hashCode()` comme C1.

---

### C4. `AllocationResult` n'a pas d'`equals()`

**Fichier :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/service/AllocationResult.java:31`

Toujours le même pattern cassé : `hashCode()` redéfini, `equals()` absent.

**Fix le plus propre :** convertir en `record` puisque c'est un value object pur et immuable :

```java
public record AllocationResult(LocationId locationId, int quantity) {
    public AllocationResult {
        Objects.requireNonNull(locationId, "locationId cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
    }
    public static AllocationResult of(LocationId locationId, int quantity) {
        return new AllocationResult(locationId, quantity);
    }
}
```

Accessoirement : supprime aussi l'import `java.util.UUID` inutilisé.

---

### C5. Constructeurs privés des `*Id` sans null-check

**Fichiers :** `SaleId.java`, `UserId.java`, `MovementId.java`, `ShopId.java`, `LocationId.java` (tous sauf `ProductId` et `CategoryId`).

Le constructeur privé accepte silencieusement `null`. Résultat : `SaleId.of(null)` construit un objet invalide, qui explosera à la première opération `equals`/`toString` par `NullPointerException` — message peu utile, loin de la cause.

**Fix :** uniformiser sur le pattern déjà présent dans `ProductId` :

```java
private LocationId(UUID value) {
    this.value = Objects.requireNonNull(value, "value cannot be null");
}
```

---

### C6. `Money.hashCode()` passe par `amount.doubleValue()`

**Fichier :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/model/shared/Money.java:69`

```java
return Objects.hash(amount.doubleValue(), currency);
```

Le commentaire `// Astuce pour le hash de BigDecimal` masque un bug subtil. `equals()` utilise `compareTo` sur `BigDecimal` (donc `10.0` et `10.00` sont égaux), mais `hashCode()` passe par `doubleValue()` : précision perdue au-delà de ~15 chiffres significatifs. Deux `Money` "equals" peuvent produire des hashCode différents dans les cas extrêmes → contrat `equals/hashCode` cassé.

C'est peu probable pour des prix en euros d'une boutique auto (pas des montants à 18 décimales), mais en entretien technique ou revue de code, ça se voit.

**Fix :**

```java
@Override
public int hashCode() {
    // stripTrailingZeros rend 10.0 et 10.00 hash-équivalents,
    // sans perte de précision
    return Objects.hash(amount.stripTrailingZeros(), currency);
}
```

---

## 🟠 IMPORTANT — 10 points

### I1. `StorageLocationRepository.findById()` ne retourne pas `Optional`

**Fichier :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/model/stock/ports/StorageLocationRepository.java:11`

```java
StorageLocation findById(LocationId locationId);
```

Retour non-`Optional` alors que `ProductRepository.findById()` retourne bien `Optional<Product>`. Incohérence de contrat, et surtout : que se passe-t-il si la location n'existe pas ? Contrat non défini → chaque implémentation fera ce qu'elle veut (null, exception, ...).

Dans `TransferStockUseCase.execute()` et `SellProductUseCase.sell()`, tu appelles `findById()` puis utilises directement l'objet sans check. Si l'infra retourne `null`, NPE.

**Fix :** aligner sur `ProductRepository` :

```java
Optional<StorageLocation> findById(LocationId locationId);
```

…et traiter le `Optional.orElseThrow(() -> new StorageLocationNotFoundException(locationId))` dans les use cases. Ça te force aussi à créer une exception métier propre (qui mappera en 404 au niveau REST).

---

### I2. Package `Dto` avec majuscule dans le domaine

**Chemin :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/model/sale/Dto/`

Convention Java : les packages sont tout en minuscules. Le reste du projet respecte ça (`dto/` en application, `event/`, `port/`, `ports/`). Seul ce package casse la règle.

**Plus profondément :** ces classes (`SaleLineDto`, `SaleLineRequest`) ne devraient probablement pas être appelées "DTO". Un DTO est un objet de transfert **entre couches**. `SaleLineRequest` est un **command input pour la factory** `Sale.create()` — il vit dans le domaine et c'est légitime. `SaleLineDto` est une **projection de lecture** de `Sale.getLines()` — c'est un vrai DTO qui pourrait vivre en application.

**Fix proposé :**

- Déplacer `SaleLineRequest` directement dans `domain/model/sale/` (pas dans un sous-package), le renommer `NewSaleLineCommand` ou `SaleLineInput` pour clarifier.
- Déplacer `SaleLineDto` dans `stock-application/src/main/java/com/aliCheikh/stock/application/dto/` (ou dans une future couche `read-model`). Ça retire une dépendance circulaire potentielle entre le domaine et ses "vues".

Si tu n'as pas le temps : au strict minimum, renomme le dossier en `dto/` (minuscules).

---

### I3. `SellProductCommand` ne gère qu'un seul produit

**Fichier :** `stock-application/src/main/java/com/aliCheikh/stock/application/dto/SellProductCommand.java`

```java
public record SellProductCommand(ProductId productId, int quantity, UserId sellerId, ShopId shopId) {}
```

Ton agrégat `Sale` est bien conçu pour accepter plusieurs lignes (`Sale.create(sellerId, List<SaleLineRequest>)`), mais la commande applicative ne prend qu'un produit. Résultat : `SellProductUseCase.sell()` construit une vente à une seule ligne, puis la sauvegarde. Une vente = un produit. C'est incohérent avec le modèle métier (un client qui achète filtre + bougies + huile = 3 use cases successifs ? ou 3 ventes distinctes ? la facture sera fausse).

**Fix :** remplacer par une commande multi-lignes :

```java
public record SellProductCommand(
        UserId sellerId,
        ShopId shopId,
        List<SellLineCommand> lines
) {
    public SellProductCommand {
        Objects.requireNonNull(sellerId, "sellerId cannot be null");
        Objects.requireNonNull(shopId, "shopId cannot be null");
        Objects.requireNonNull(lines, "lines cannot be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("at least one line is required");
        }
    }
}

public record SellLineCommand(ProductId productId, int quantity) {
    public SellLineCommand {
        Objects.requireNonNull(productId, "productId cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
    }
}
```

Et dans `SellProductUseCase.sell()`, boucler sur les lignes pour allouer, décrémenter, créer les `StockMovement`, avant de créer UNE `Sale` agrégeant toutes les lignes.

**C'est l'évolution la plus importante du code**, parce qu'elle change ton contrat d'API aussi. Ma proposition d'OpenAPI en tient compte.

---

### I4. `ReceiveStockUseCase.calculateGlobalStock()` appelle `findAll()`

**Fichier :** `stock-application/src/main/java/com/aliCheikh/stock/application/usecase/ReceiveStockUseCase.java:146`

```java
return storageLocationRepository.findAll().stream()
        .mapToInt(loc -> loc.getStockLevel(productId))
        .sum();
```

`findAll()` ramène **toutes les storage locations de toutes les boutiques**. Au-delà de la première boutique, ça scanne pour rien. Ne scale pas si multi-boutiques (que tu as explicitement prévu dans l'architecture).

`SellProductUseCase.calculateGlobalStock()` fait bien, lui : `findByShopId(command.shopId())`.

**Fix :** ajouter `shopId` à `ReceiveStockCommand` et utiliser `findByShopId()` :

```java
public record ReceiveStockCommand(
        String productReference,
        ProductInfo newProductInfo,
        ShopId shopId,         // ← ajouter
        UserId userId,
        List<TargetLocation> distributions
) { ... }
```

**Bonus :** le jour où tu passes en SQL, tu pourras ajouter une méthode dédiée `int sumStockForProduct(ProductId, ShopId)` sur le repository pour éviter de charger tous les agrégats en mémoire juste pour faire une somme — query CQRS.

---

### I5. Pas de frontière transactionnelle

**Fichiers :** les trois use cases.

`SellProductUseCase.sell()` fait dans l'ordre : save(sale) → pour chaque allocation : save(location) + save(movement). Si `save(movement)` échoue au milieu, tu as une `Sale` enregistrée, une partie des locations décrémentées, et des mouvements partiels. Données incohérentes.

Le domaine ne doit pas connaître les transactions (correct), mais l'**application** doit les démarquer. Deux approches standards :

**Option A (simple, Spring-friendly) :** laisser l'annotation `@Transactional` arriver quand tu colleras Spring dans `stock-infrastructure`. Le use case reste tel quel, l'intercepteur ouvre/commit/rollback autour de `sell()`.

**Option B (plus pur, découplé de Spring) :** introduire un port `TransactionManager` dans `stock-application/port/` avec une méthode `execute(Runnable)`, et envelopper la logique du use case dedans.

**Recommandation pour ton contexte :** option A pour aller vite, mais **documente** explicitement dans la Javadoc du use case : `// MUST be invoked within a transactional boundary`. Ça évite les trous si quelqu'un appelle le use case sans passer par le controller Spring.

---

### I6. `StockMovement.createTransfer()` lance `IllegalArgumentException` au lieu de `InvalidMovementException`

**Fichier :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/model/movement/StockMovement.java:99-105`

```java
if (destinationLocationId == null) {
    throw new IllegalArgumentException("destinationLocationId cannot be null");
}
if (sourceLocationId == null) {
    throw new IllegalArgumentException("sourceLocationId cannot be null");
}
```

Incohérent : `createEntry()` et `createExit()` utilisent `InvalidMovementException` avec un `MovementErrorReason` typé pour la même classe de problème (source/destination manquant). Un handler REST qui mappe `InvalidMovementException` → 409 Conflict va laisser passer ces deux cas en 500 Internal Error.

**Fix :**

```java
// dans MovementErrorReason.java
TRANSFER_MISSING_SOURCE,
TRANSFER_MISSING_DESTINATION,
TRANSFER_SAME_SOURCE_DESTINATION;

// dans createTransfer()
if (destinationLocationId == null) {
    throw new InvalidMovementException(
        MovementErrorReason.TRANSFER_MISSING_DESTINATION,
        "destinationLocationId cannot be null");
}
if (sourceLocationId == null) {
    throw new InvalidMovementException(
        MovementErrorReason.TRANSFER_MISSING_SOURCE,
        "sourceLocationId cannot be null");
}
```

---

### I7. `ReceiveStockCommand` ne valide rien dans son constructeur compact

**Fichier :** `stock-application/src/main/java/com/aliCheikh/stock/application/dto/ReceiveStockCommand.java`

Les autres commands ont un compact constructor avec guards (`TransferStockCommand` notamment). `ReceiveStockCommand` laisse passer n'importe quoi :

- `productReference` vide ou null → `productRepository.findByReference(null)` va exploser loin d'ici
- `distributions` null ou vide → on passe silencieusement sans rien recevoir
- `userId` null → NPE différée
- `distributions` contenant des quantités ≤ 0 → déjà validé par `TargetLocation`, OK

**Fix :**

```java
public ReceiveStockCommand {
    Objects.requireNonNull(productReference, "productReference cannot be null");
    if (productReference.isBlank()) {
        throw new IllegalArgumentException("productReference cannot be blank");
    }
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(distributions, "distributions cannot be null");
    if (distributions.isEmpty()) {
        throw new IllegalArgumentException("distributions cannot be empty");
    }
    // newProductInfo est volontairement nullable
}
```

---

### I8. `SellProductUseCase` re-fetche les storage locations pour calculer le global stock

**Fichier :** `stock-application/src/main/java/com/aliCheikh/stock/application/usecase/SellProductUseCase.java:95 + 122-126`

```java
int globalStock = calculateGlobalStock(command); // refait findByShopId
```

Après avoir décrémenté les locations et les avoir sauvées, tu refais un `findByShopId` pour refaire la somme. Deux I/O pour une info que tu peux calculer avec ce que tu as en main.

**Fix :**

```java
// Tu as chargé chaque location par allocation.getLocationId()
// et tu l'as toujours en mémoire. Calcule avant de save :
//
// int globalStockAfter = locations.stream()
//     .mapToInt(loc -> loc.getStockLevel(command.productId()))
//     .sum();
```

À l'échelle d'une petite boutique ça ne change rien, mais c'est le **genre de détail qui fait la différence en revue de code en entreprise** et en entretien.

---

### I9. Versions de dépendances hardcodées dans chaque pom

**Fichiers :** `stock-domain/pom.xml`, `stock-application/pom.xml`

JUnit 5.11.0, AssertJ 3.26.3, Mockito 5.17.0 sont déclarées module par module. Le jour où Mockito passe en 5.18.0 et où tu upgrades dans un seul module, divergence silencieuse → tests qui ne compilent plus ensemble.

**Fix :** centraliser dans le parent POM (`auto-stock-management/pom.xml`) :

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.11.0</version>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>3.26.3</version>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.17.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Et dans chaque module, tu déclares les dépendances **sans version** :

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

---

### I10. `StorageLocation.pullEvents()` : pas de protection thread-safety ni de sémantique claire en cas d'exception

**Fichier :** `stock-domain/src/main/java/com/aliCheikh/stock/domain/model/stock/StorageLocation.java:118-122`

```java
public List<DomainEvent> pullEvents() {
    List<DomainEvent> events = new ArrayList<>(this.domainEvents);
    this.domainEvents.clear();
    return events;
}
```

Deux points :

1. **Thread-safety :** l'agrégat est manipulé dans un thread unique (transaction), donc OK en l'état, mais si tu pars sur du reactive/parallèle plus tard, la liste `domainEvents` doit être concurrente ou le pull atomique.
2. **Sémantique d'échec :** si `pullEvents()` est appelé mais que la persistance échoue ensuite, les events sont **perdus**. Le pattern standard (Evans / Vernon) est : on ne pull qu'**après** commit réussi. Ça doit être la règle dans tes use cases. Aujourd'hui dans `SellProductUseCase`, tu pulls avant `save(location)`. Si le save échoue, l'agrégat est gardé en mémoire peut-être, mais les events sont déjà dans la liste `eventsToPublish` → risque de publier des events d'une transaction rollbackée.

**Fix pragmatique pour ton scope :**

- Déplacer le `pullEvents()` APRÈS le `save(location)` dans `SellProductUseCase` (protège du cas où save lève)
- Et publier les events **à la toute fin**, après tous les saves → déjà le cas, OK.

Mais l'ordre exact dans la boucle reste à corriger. Aujourd'hui :

```java
location.decreaseStock(...);
eventsToPublish.addAll(location.pullEvents()); // events extraits
storageLocationRepository.save(location);       // save peut échouer
```

Devrait être :

```java
location.decreaseStock(...);
storageLocationRepository.save(location);       // save d'abord
eventsToPublish.addAll(location.pullEvents()); // events seulement si save OK
```

---

## 🟡 NICE-TO-HAVE — 12 points

### N1. `DomainEvent` : mot-clé `public` redondant sur la méthode d'interface

```java
public interface DomainEvent {
    public LocalDateTime getOccurredAt(); // "public" est implicite
}
```

Les méthodes d'interface sont implicitement `public abstract`. Supprime le `public`.

---

### N2. Import inutilisé dans `AllocationResult.java`

```java
import java.util.UUID; // jamais utilisé
```

Nettoyage d'imports à faire dans tout le projet (l'IDE te fera ça en un raccourci).

---

### N3. Lignes vides multiples en fin de fichier

`StockMovement.java` se termine par 5 lignes vides. `StockLevel.java` a 3 blocs de lignes vides internes. Cosmétique, mais c'est du bruit. Active `Reformat Code` IntelliJ en pre-commit.

---

### N4. Mélange français / anglais dans les commentaires et messages

Exemples :

- `SellProductUseCase` : commentaires en anglais, `ReceiveStockUseCase` idem, mais `StockAllocationService` a "Récupérer", "Calcul du stock", "On lance l'exception riche !"
- `TransferStockCommand` : `"La quantité à transférer doit être strictement positive"` (FR)
- Les autres exceptions : messages en anglais

**Reco :** tu es dans un projet portfolio / stage — vise l'anglais partout dans le code (commentaires, messages d'exception, javadoc). Les noms DDD peuvent rester en anglais métier (`Sale`, `Shop`, `ReceivingService`) avec un glossaire français dans la doc (que tu as déjà via `ubiquitous-language.adoc`).

---

### N5. Pas d'`equals/hashCode` sur les aggregate roots

`Product`, `StorageLocation`, `Shop`, `Category`, `Sale`, `StockMovement`, `User` n'ont pas d'equals/hashCode. La règle DDD : **deux aggregate roots sont égaux ssi leur ID est égal**. Sans ça, deux instances reconstruites depuis la DB ne sont jamais "égales" → pièges dans tests, dans cache, dans deduplication.

**Fix type (pour `Product`) :**

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Product other = (Product) o;
    return productId.equals(other.productId);
}

@Override
public int hashCode() {
    return productId.hashCode();
}
```

À répliquer sur chaque aggregate root.

---

### N6. `StorageLocation.label` validé avec `IllegalArgumentException` au lieu d'une exception métier

Dans `StorageLocation`, cinq guards utilisent `IllegalArgumentException` brut :

```java
if (locationId == null) throw new IllegalArgumentException("locationId cannot be null");
```

alors que le reste du domaine utilise des exceptions métier typées (`InvalidProductNameException`, `InvalidCategoryNameException`, `InvalidLowStockIndicatorException`). Incohérence.

**Fix :** soit créer `InvalidStorageLocationException` avec un enum de raison, soit accepter que les null-checks de construction restent techniques. La règle fréquente : **`IllegalArgumentException` pour les contrats techniques** (null), **exception métier typée pour les invariants du domaine** (stock < 0, nom vide, etc.). Ton code applique déjà cette règle pour `lowStockIndicator < 0` → `InvalidLowStockIndicatorException` ✓. Pour les autres (`label` vide notamment), c'est discutable — un label vide est un invariant métier, pas technique.

**Reco pragmatique :** garde les null-checks en `IllegalArgumentException`, mais le `label.isBlank()` devrait lever une `InvalidStorageLocationLabelException`.

---

### N7. `Sale.SaleLineItem` est une **classe statique imbriquée privée** mais sa doc la présente comme une "Entité interne"

Dans `Sale.java:52`, `SaleLineItem` est `private static class`. Pas de souci technique, c'est propre. Mais la doc Antora (`aggregate-sale.adoc`) la présente avec `<<Entity>>`.

Debate DDD : est-ce une Entity (a une identité positionnelle dans sa sale) ou un Value Object (comparable par valeur) ? Tes commentaires hésitent. Le code la traite en value object de facto (pas d'id).

**Reco :** la qualifier explicitement dans le code :

```java
/**
 * Value object interne, identifié positionnellement par sa Sale parente.
 * N'a pas d'existence hors de l'agrégat Sale.
 */
private static final class SaleLineItem { ... }
```

(le `final` en bonus — ça ne doit pas être étendu)

---

### N8. `Sale.create()` — `reduce(Money::add).orElseThrow()` sans message

```java
Money totalAmount = internalLines.stream()
        .map(SaleLineItem::getLineTotal)
        .reduce(Money::add)
        .orElseThrow();
```

Techniquement inatteignable (tu as validé que `lineRequests` n'est pas vide juste avant), mais si jamais : message générique `NoSuchElementException`. Ajoute un message défensif :

```java
.orElseThrow(() -> new IllegalStateException(
    "Sale.create invariant violated: lineRequests empty"));
```

Ou mieux, utilise un identity element pour éviter l'orElseThrow :

```java
// Si tu ajoutes Money.zero(Currency) comme factory :
Money totalAmount = internalLines.stream()
        .map(SaleLineItem::getLineTotal)
        .reduce(Money.zero(currency), Money::add);
```

---

### N9. Javadoc incomplète

Seul `ReceiveStockUseCase` a une Javadoc de classe. Les autres use cases, les agrégats, les services → rien. Pour un projet portfolio visé stage, Javadoc sur toutes les **classes publiques** et toutes les **méthodes publiques** des agrégats / use cases / ports donne un gros signal de sérieux.

Format minimal :

```java
/**
 * Use case: sell products to a customer.
 *
 * <p>Allocates stock across shop locations, creates the Sale,
 * records one EXIT movement per allocated location, and emits
 * SaleCompleted + LowStockAlert (conditional) + ShopFloorLow (conditional)
 * domain events.
 *
 * <p>MUST be invoked within a transactional boundary (orchestrated by the
 * infrastructure layer).
 */
public class SellProductUseCase { ... }
```

---

### N10. `Sale.getLines()` recrée une liste à chaque appel

```java
public List<SaleLineDto> getLines() {
    return this.lines.stream()
            .map(line -> new SaleLineDto(...))
            .toList();
}
```

Pour une Sale lue N fois (typique dans un endpoint REST qui boucle sur les ventes du jour), tu refais la projection à chaque appel. Sans impact à ta taille, mais en principe une projection devrait être cachée ou calculée par un read-model externe à l'agrégat.

**Reco :** laisse tel quel, mais note-le comme candidat à un projection/read-model CQRS si perf devient un souci.

---

### N11. `DomainEvent` n'a pas d'`eventId`

Pour du messaging asynchrone, un event doit pouvoir être dédupliqué, ré-rejoué, tracé. `eventId : UUID` + `eventVersion : int` sont attendus. Pas urgent pour ton scope (sync in-process), mais pour un vrai système prod, c'est incontournable.

À mentionner dans la doc comme "évolution prévue" si question en soutenance.

---

### N12. Tests use case minimaux

Tu as 3 tests pour 3 use cases, mais la complexité réelle de `SellProductUseCase` (7 étapes, 3 events conditionnels) mérite ~10 tests. Idem `ReceiveStockUseCase` (branche "produit existant" vs "nouveau produit", événement `StockReplenished` conditionnel).

**Reco :** pour chaque use case, couvre au minimum :

- Happy path nominal
- Product not found
- Stock insuffisant (pour Sell et Transfer)
- Event X émis (chaque event, un test)
- Event X NON émis quand la condition n'est pas remplie
- Ordonnancement des saves (ex : Sale sauvée avant movement)

---

## Architecture — points d'attention globaux

### A1. Module `stock-infrastructure` vide

Normal à ce stade, mais quand tu le rempliras :

- **Un adapter par port domain/app** : `JpaProductRepository implements ProductRepository`, etc.
- **Mapper domaine ↔ entité JPA séparé** (ne pollue pas les aggregates avec `@Entity` — c'est l'erreur classique qui casse la neutralité du domaine).
- **`SpringEventPublisher implements EventPublisher`** en premier rideau, remplaçable par Kafka/RabbitMQ plus tard.
- **Controllers REST** qui ne font QUE : parser le DTO HTTP → construire le Command → appeler le use case → mapper la réponse.

### A2. Pas d'abstraction `AggregateRoot`

Option légère pour uniformiser `pullEvents()` entre `StorageLocation`, `Sale` (si un jour tu lui ajoutes des events), etc. :

```java
public abstract class AggregateRoot {
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    protected final void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    public final List<DomainEvent> pullEvents() {
        List<DomainEvent> snapshot = List.copyOf(this.domainEvents);
        this.domainEvents.clear();
        return snapshot;
    }
}
```

Bonus pédagogique en soutenance : ça montre que tu connais les patterns DDD classiques.

### A3. `EventPublisher` publie une `List<DomainEvent>` d'un coup

OK, mais quand tu brancheras un bus asynchrone, la publication atomique d'une liste de N events n'est pas toujours garantie. Regarde **transactional outbox pattern** si tu veux pousser la doc architecturale.

---

## Checklist avant de coder le controller REST

Dans l'ordre de priorité :

1. ✅ Fixer C1, C2, C3, C4, C5 (contrats equals/hashCode et null-checks IDs) — **~30 min**
2. ✅ Fixer C6 (Money.hashCode) — **5 min**
3. ✅ Fixer I1 (findById → Optional) et propager dans les use cases — **~20 min + tests**
4. ✅ Fixer I2 (renommer `Dto` → `dto`) — **5 min**
5. ✅ Fixer I3 (SellProductCommand multi-lignes) — **~1h + refactor use case + tests**
6. ✅ Fixer I4, I7 (ReceiveStockCommand avec shopId + validations) — **~20 min**
7. ✅ Fixer I5 (annotation transactionnelle ou doc explicite) — **arrivera avec Spring**
8. ✅ Fixer I6 (createTransfer InvalidMovementException) — **10 min**
9. ✅ Fixer I8 (éviter le double fetch) — **10 min**
10. ✅ Fixer I9 (dependencyManagement dans parent POM) — **10 min**
11. ✅ Fixer I10 (ordonnancement save → pullEvents) — **5 min**
12. ✅ Nettoyages N1-N4 en one-shot via formatter IntelliJ — **10 min**
13. ✅ Ajouter equals/hashCode sur aggregate roots (N5) — **30 min**
14. ✅ Étoffer tests use cases (N12) — **~2h**

Ensuite seulement, attaque `stock-infrastructure/` : Spring Boot, controllers, JPA, et le câblage avec ta nouvelle OpenAPI spec.

---

## Ce qui est déjà très bien

Pour équilibrer, il faut aussi dire que :

- La **séparation domain / application / infrastructure** est respectée à la lettre. Aucune annotation Spring ni JPA ne pollue le domaine.
- Les **factory methods** pour `StockMovement.createEntry/Exit/Transfer` sont un vrai pattern DDD, pas décoratif.
- **`StockAllocationService`** avec le fail-fast sur le stock global avant l'allocation location-par-location : propre.
- **`StorageLocation.pullEvents()`** : pattern event-from-aggregate exécuté correctement (rare à ce stade de formation).
- **Guard clauses systématiques** dans tous les constructeurs.
- **Exceptions métier riches en contexte** (`InsufficientStockException` porte `productId`, `available`, `requested`) : ça rend l'observabilité et le mapping REST triviaux.
- **Tests domaine** (9 fichiers) : c'est déjà un bon socle, bien au-dessus de ce que j'ai vu dans beaucoup de projets de M2.
- **Doc Antora** : structure propre, PlantUML, langage ubiquitaire documenté. Très professionnel.

Le code est sur la bonne voie. Les points critiques ci-dessus sont des bugs de surface (contrats Java) et de la dette d'API applicative (mono vs multi-ligne) — pas des erreurs de conception. L'architecture tient.
