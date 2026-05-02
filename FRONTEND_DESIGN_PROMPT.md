# Prompt pour Claude Design — Frontend Angular

> Copie le bloc ci-dessous dans Claude Design. Il est auto-contenu : il porte
> le contexte produit, la stack, les anti-patterns, le vocabulaire métier, et
> l'inventaire complet des écrans à concevoir.

---

## 🟦 PROMPT À COPIER

Tu conçois l'interface frontend d'un logiciel de **gestion de stock pour une
boutique de pièces détachées automobiles**. Le backend est déjà construit
(Java / architecture hexagonale / DDD) ; ton travail est le **design UI/UX
complet** de l'application web.

### 1. Contexte produit — à ne PAS romantiser

Ce n'est pas un "SaaS inspirant". C'est un **outil de travail** utilisé
chaque jour par :

- **Le propriétaire (OWNER)** — il achète la marchandise (souvent à l'import,
  ex. depuis Dubaï), suit ses marges, voit les alertes de stock bas, décide
  des transferts entre réserve et magasin.
- **Le vendeur (SELLER)** — au comptoir toute la journée, tape les ventes le
  plus vite possible, trouve une référence par code ou par nom de pièce,
  signale quand le rayon est vide.

Les utilisateurs ne lisent pas les écrans, ils les **scannent**. Vitesse >
esthétique. Densité d'information > espacement aéré. Clavier > souris. La
boutique est souvent bruyante, éclairée au néon : **les contrastes comptent**.

### 2. Stack et contraintes techniques

- **Angular 17+** (standalone components, signals, control flow `@if`/`@for`).
- **Angular Material** **interdit en bloc**. Utilise du CSS custom ou **Tailwind 4** ;
  pas de composants pré-packagés qui imposent leur style.
- Composants : structure propre `feature/`, `shared/`, `core/`. Pas de
  "ui-kit" générique ; chaque composant est pensé pour son contexte.
- Le backend expose une API REST versionnée `/api/v1/…` (spec OpenAPI 3.1
  jointe plus bas). Toute la donnée vient de là.
- **Realtime** : prévoir les emplacements pour des événements domaine poussés
  (SSE / WebSocket). Quand `ShopFloorLow` ou `LowStockAlert` tombent, l'UI doit
  réagir sans refresh — bannière contextuelle, pas notification système.

### 3. Direction artistique — ce qu'on refuse

**Rejette tout ce qui ressemble à :**

- Dégradés violet → rose, icônes Lucide partout, cartes arrondies `rounded-2xl`
  avec ombre douce, illustrations 3D bullshit "people walking with a laptop".
- Les dashboards SaaS génériques (type Vercel/Linear mal copié) avec leurs
  KPI cards identiques, leurs graphiques colorés et leur whitespace disproportionné.
- Le style "Claude AI chat bubble" — fond crème, bulles arrondies, typo serif.
- Les tableaux mous sans règles de lignes, où on devine les colonnes.
- Les modales centrées qui cachent le contexte en dessous.

### 4. Direction artistique — ce qu'on veut

**Inspirations lointaines, pas à copier :**

- La densité tabulaire et le clavier-first de **Linear** et **Superhuman**.
- La sobriété industrielle des cockpits pro (SAP neuve, pas ancienne ;
  Bloomberg Terminal pour l'idée, pas pour le look).
- La lisibilité des outils de logistique type **Flexport** ou **Samsara** —
  chiffres énormes, unités claires, zéro décoration.
- Une touche d'identité "boutique automobile" : pas d'émoji voiture ni
  d'illustration, mais une palette mécanique discrète (acier, orange sécurité,
  fond off-white ou dark gris charbon — choisis et assume).

**Règles concrètes :**

- Typo : une sans-serif technique (Inter, IBM Plex Sans, ou Geist) +
  une mono pour les références produit et les montants (JetBrains Mono ou
  Geist Mono). Deux fontes max.
- Hiérarchie par **poids et taille**, pas par couleur. La couleur est
  réservée au sens métier (rouge = alerte stock critique, ambre = seuil
  approché, vert = réapprovisionné).
- Tableaux avec rangs zébrés subtils, règles de lignes fines, tri par
  colonne, recherche en tête, raccourci `/` pour focus search partout.
- **Raccourcis clavier affichés**. `N` = nouvelle vente, `R` = réception,
  `T` = transfert, `G S` = aller aux ventes, `G P` = aller aux produits,
  `?` = afficher tous les raccourcis.
- Transitions : discrètes, **≤ 150 ms**, pas de rebond, pas d'élastique.
- Mode sombre de qualité égale au mode clair (pas un simple `invert`).

### 5. Vocabulaire métier — à utiliser TEL QUEL dans l'UI

Ces mots viennent du domaine (Ubiquitous Language). Ne les reformule pas.

| Terme UI (FR)    | Concept backend                                                      | Notes                                                            |
|------------------|----------------------------------------------------------------------|------------------------------------------------------------------|
| Produit          | `Product`                                                            | Identifié par sa référence (ex. `BRK-PAD-2017-RENAULT`).         |
| Référence        | `reference`                                                          | Code alphanumérique unique, toujours en mono.                    |
| Emplacement      | `StorageLocation`                                                    | Deux types : **Magasin** (SHOP_FLOOR) / **Réserve** (BACKSTOCK). |
| Shop / Boutique  | `Shop`                                                               | Un owner peut avoir plusieurs shops ; filtre global en haut.     |
| Stock global     | somme des `StockLevel` d'un produit sur toutes les locations du shop |                                                                  |
| Seuil global     | `minimumGlobalThreshold`                                             | Déclenche `LowStockAlert`.                                       |
| Seuil magasin    | `lowStockIndicator` sur SHOP_FLOOR                                   | Déclenche `ShopFloorLow`.                                        |
| Vente            | `Sale`                                                               | Multi-lignes possible, prix unitaire figé au moment de la vente. |
| Réception        | `ReceiveStock` (use case)                                            | Entrée depuis fournisseur, peut créer un produit à la volée.     |
| Transfert        | `TransferStock` (use case)                                           | Backstock → Shop floor (ou inverse), même shop uniquement.       |
| Mouvement        | `StockMovement`                                                      | Journal d'audit : `ENTRY`, `EXIT`, `TRANSFER`.                   |
| Alerte stock bas | `LowStockAlert`                                                      | Stock global sous le seuil.                                      |
| Rayon vide       | `ShopFloorLow`                                                       | Magasin sous le seuil, réserve peut-être OK.                     |

Monnaie : format `{ amount: "45.90", currency: "EUR" }` — `amount` est une
string (précision BigDecimal). Affiche toujours avec la devise (jamais juste
un nombre).

### 6. Rôles & permissions

- **OWNER** voit tout. Seul rôle qui voit les montants de marge, lance les
  réceptions fournisseur, accède à l'admin (utilisateurs, shops, catégories).
- **SELLER** voit le catalogue, les niveaux de stock, enregistre des ventes,
  peut demander un transfert, voit ses propres ventes. Les champs financiers
  sensibles (marge, coût d'achat) sont masqués côté backend — ne les dessine
  pas pour lui.

Le filtre ne se fait **pas** par URL différente. Les mêmes écrans, des
champs en moins. Prévoir la variante visuelle (même gabarit, colonnes
cachées).

### 7. Inventaire complet des écrans

Pour chaque écran : layout, états (vide / chargement / erreur / succès),
interactions clavier, et responsive (≥ 1280px prioritaire ; le mobile sert
au scan rapide de référence, pas à la saisie).

#### 7.1 — Authentification

- **Login** : email + mot de passe. Pas d'inscription publique (comptes
  créés par l'OWNER). Message d'erreur précis (compte désactivé vs mot de
  passe faux). Raccourci `Enter` pour soumettre.

#### 7.2 — Shell applicatif (présent sur tous les écrans)

- Barre latérale fine avec icônes + label. Sections :
  Ventes · Stock · Produits · Mouvements · Admin (OWNER).
- Header : recherche globale `⌘K` (produit par ref ou nom, vente par id,
  location), sélecteur de shop (si plusieurs), badge d'alertes non lues,
  avatar compte.
- Aucun footer décoratif.

#### 7.3 — Dashboard (landing après login)

Variante **OWNER** :

- Bandeau d'alertes : `LowStockAlert` actifs (rouges), `ShopFloorLow`
  actifs (ambre), actionnables d'un clic.
- Chiffres du jour : ventes réalisées (compte + total €), réceptions reçues,
  transferts exécutés. Chiffre énorme, label petit.
- Liste "à traiter" : produits sous seuil global, rayons vides à
  réapprovisionner depuis la réserve, dernières réceptions.
- Graphique de ventes sur 7 jours — **un seul**, sobre, pas de dashboard à
  12 widgets.

Variante **SELLER** :

- Gros bouton « Nouvelle vente » (raccourci `N`).
- Mes 10 dernières ventes.
- Alertes `ShopFloorLow` uniquement (ce que le vendeur peut agir).
- Pas de chiffres financiers agrégés.

#### 7.4 — Nouvelle vente (la plus critique — pensée POS)

- Écran plein, pas une modale.
- Zone gauche : scan / saisie de référence. Auto-focus permanent sur le
  champ référence. Appuyer `Enter` ajoute une ligne avec quantité = 1.
  La référence reste mise en évidence en mono. Deuxième `Enter` rapide =
  incrémente la quantité de la dernière ligne.
- Zone droite : lignes de vente empilables, éditable en place (quantité,
  suppression). Prix unitaire figé à l'ajout (snapshot).
- Bas d'écran : total en très gros, bouton **Valider la vente** (`⌘+Enter`).
- Erreurs possibles : `PRODUCT_NOT_FOUND`, `STOCK_INSUFFICIENT`.
  Afficher inline, au dessus de la ligne concernée, en rouge, pas en
  toast. Garder le champ référence focus.
- Après validation : confirmation en 1 seconde (pas de modal de succès
  qui bloque), reset automatique, focus revient sur référence.
- État "prix caché pour SELLER ? non — il doit voir les prix pour annoncer
  le total au client". Montrer `unitPrice` et `subtotal`, pas la marge.

#### 7.5 — Historique des ventes

- Table dense, pagination `page/size/sort`.
- Filtres en tête : période (from/to), vendeur, shop.
- Colonnes : `saleId` (court, mono), date, vendeur, nb lignes, total.
- Clic sur une ligne → panneau latéral droit (pas modale) avec le détail
  des lignes.
- Raccourci `J`/`K` pour naviguer ligne par ligne.

#### 7.6 — Détail d'une vente

- Panneau ou page dédiée. Lignes, mouvements de stock générés
  (provenance : magasin ou réserve, quantité prise d'où), horodatage.
- Total avec décomposition par ligne.
- Pas d'annulation pour le moment (pas dans le backend — ne pas dessiner de
  bouton qui n'est pas supporté).

#### 7.7 — Réception de stock (OWNER)

- Formulaire en deux temps explicites :
    1. **Référence** : saisie. Si produit connu → affiche nom/catégorie en
       lecture seule. Si inconnu → bloc "Nouveau produit" apparaît
       (nom, catégorie, prix unitaire, seuil global). La référence saisie
       devient la référence du produit, non modifiable.
    2. **Répartition** : liste des emplacements du shop sélectionné, avec
       champ quantité. Total calculé en live en bas. Minimum 1 ligne > 0.
- Bouton **Enregistrer la réception** (`⌘+Enter`).
- Feedback : badge "Produit créé" si création à la volée, toast discret
  "Réception enregistrée · 2 mouvements créés".
- États d'erreur : validation champ par champ (422) + erreurs RFC 7807
  globales (format `{ title, detail, code }`).

#### 7.8 — Transfert interne

- Très simple, monoline :
  Produit (recherche par ref) → Source → Destination → Quantité.
- Affiche en temps réel, sous chaque location, le stock actuel pour ce
  produit. Source et destination du **même shop** uniquement (filtrer la
  liste).
- Interdit source == destination (désactive le bouton, message inline).
- Aperçu avant validation : "15 → 5 en réserve ⇒ 1 → 11 en magasin".
- `⌘+Enter` valide.

#### 7.9 — Catalogue produits

- Table paginée : référence (mono), nom, catégorie, prix unitaire, stock
  global, état (OK / sous seuil / critique).
- Recherche textuelle (ref ou nom). Filtres catégorie.
- Clic → page produit.

#### 7.10 — Page produit

- En-tête : nom, référence, catégorie, prix unitaire, seuil global.
- Bloc **Répartition stock par emplacement** : pour chaque location du
  shop, une ligne avec quantité actuelle et type (Magasin / Réserve).
  Barre discrète indiquant le rapport au seuil.
- Bloc **Mouvements récents** pour ce produit (20 derniers), avec type
  coloré : ENTRY vert, EXIT neutre, TRANSFER bleu.
- Actions : lancer un transfert pré-rempli avec ce produit.
- Pas de graphique "belle courbe" sans utilité — un chiffre honnête vaut
  mieux qu'un graphique fumé.

#### 7.11 — Niveaux de stock (vue transverse)

- Table : un rang = un couple produit × emplacement.
- Filtres : shop, emplacement, catégorie, ✅ "sous seuil uniquement".
- Colorer la cellule quantité selon la proximité au seuil
  (vert → ambre → rouge, monté par paliers, pas en dégradé continu).

#### 7.12 — Journal des mouvements

- Timeline ou table dense. Colonnes : date (avec groupe par jour),
  type (ENTRY / EXIT / TRANSFER, badge coloré), produit (ref mono +
  nom), quantité, emplacement (source → destination si TRANSFER),
  utilisateur, `saleId` cliquable si EXIT lié à une vente.
- Filtres : période, type, produit, emplacement.
- Export CSV (bouton discret, pas dans le hero).

#### 7.13 — Centre d'alertes

- Deux onglets : **Stock global bas** (`LowStockAlert`) / **Rayon vide**
  (`ShopFloorLow`).
- Chaque alerte est actionnable : "Lancer réception" / "Lancer transfert"
  avec formulaire pré-rempli.
- Une alerte résolue disparaît (le backend émet `StockReplenished`).
  Animation de sortie ≤ 150 ms, pas de "poof".

#### 7.14 — Administration (OWNER uniquement)

- **Utilisateurs** : liste, création, activation/désactivation, rôle.
- **Shops** : liste, création.
- **Emplacements** (par shop) : liste, création, édition du
  `lowStockIndicator` pour les SHOP_FLOOR.
- **Catégories** : liste, création simple.
- Formulaires compacts, validation inline, raccourcis `⌘+S` pour
  enregistrer.

#### 7.15 — Erreurs transverses

Format backend : RFC 7807. Chaque erreur a un `code` machine
(`STOCK_INSUFFICIENT`, `PRODUCT_NOT_FOUND`, `TRANSFER_CROSS_SHOP`,
`VALIDATION_FAILED`). Mappe chaque code sur un message FR court et précis
dans l'UI. Jamais de "Une erreur est survenue". Toujours quoi et pourquoi.

#### 7.16 — États vides

Pas d'illustration. Un titre court, une phrase, un bouton d'action
principal. Exemple : "Aucune vente pour cette période — Nouvelle vente".

### 8. Livrables attendus

1. **Design system minimal** : palette (clair + sombre), échelle typo,
   spacing, règles d'états (hover, focus visible, disabled, loading).
2. **Un écran complet par section** ci-dessus (7.1 à 7.14), en haute
   fidélité, avec au moins deux états par écran (normal + erreur ou
   normal + vide).
3. **Les composants clés isolés** : table dense, champ de recherche
   globale, ligne de vente éditable, bloc d'alerte, sélecteur de shop,
   sidebar, sale panel, notification inline RFC 7807.
4. **Spécifications d'interaction clavier** page par page.
5. **Notes d'implémentation Angular** : quels composants sont standalone,
   lesquels exposent des signals, quelles parties sont candidates à
   devenir OnPush / trackBy / virtual scroll.

### 9. Anti-chèque final (à te poser avant de me rendre le design)

- [ ] Est-ce qu'un vendeur pressé peut enregistrer une vente de 3 lignes
  **sans toucher la souris** ?
- [ ] Est-ce que le dashboard OWNER tient en un seul écran sans scroll sur
  1440×900 ?
- [ ] Est-ce que les couleurs de stock (ok/ambre/rouge) sont distinguables
  en daltonisme ?
- [ ] Est-ce que chaque bouton d'action **existe côté backend** ?
  (Pas de "annuler la vente", pas de "dashboard analytique", pas de
  "export PDF" non implémentés.)
- [ ] Est-ce que j'ai évité les gradients, les émojis de décoration, les
  illustrations 3D, et les cartes `rounded-2xl shadow-lg` ?
- [ ] Est-ce que le mode sombre est pensé, pas juste inversé ?

### 10. Annexes à joindre à Claude Design

- Le fichier `api/openapi.yaml` du backend (spec complète).
- Le fichier `antora-docs/modules/domain/pages/ubiquitous-language.adoc`
  (vocabulaire officiel à respecter).
- Deux ou trois captures d'écran de boutiques de pièces détachées réelles
  (comptoir, rayonnage) pour te caler sur la réalité physique du lieu —
  ne pas dessiner un Apple Store.

Tu as toutes les informations. Pas de questions ouvertes : conçois.
