# Brief front — Créances : détail, historique, et forme de la vente

## Objectif

Le patron voit aujourd'hui une liste de soldes et rien d'autre. Il ne sait pas **pourquoi** un
client lui doit 100 000 F CFA, ni **quels produits** sont concernés, ni **qui** a vendu. Et dès
qu'une créance est payée, elle disparaît de l'écran : plus aucune trace du règlement, au moment
précis où un client peut contester avoir payé.

Trois manques à combler :

1. ouvrir une créance et voir le détail de la vente ;
2. consulter l'historique des créances déjà soldées ;
3. lire, dans l'historique des mouvements, si une vente a été payée au comptoir ou faite à crédit.

Ces écrans sont réservés au rôle `OWNER`. Un vendeur ne doit ni voir l'entrée de navigation ni
pouvoir atteindre la route ; le backend répond `403` de toute façon.

Contraintes du projet, inchangées : Angular 20, composants standalone, signals, `inject()`, aucune
bibliothèque UI, réutilisation exclusive des design tokens de `src/styles.scss`. Authentification
par cookies existants.

---

## Contrat API

### Rupture à traiter en premier

`GET /api/v1/debts` et `GET /api/v1/customers/{customerId}/debts` ne renvoient **plus un tableau
nu** mais une page. Le code qui lit `response[0]` doit lire `response.content[0]`.

```json
{
  "content": [ /* Debt */ ],
  "page": { "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
}
```

### Lister les créances

`GET /api/v1/debts?status=OUTSTANDING&customerId=&page=0&size=20` → `200`

`status` vaut `OUTSTANDING` (défaut), `SETTLED` ou `ALL`. `customerId` est facultatif.
**Aucun paramètre de tri n'est accepté** : l'ordre est décidé par le serveur et dépend du statut —
les créances en cours arrivent de la plus ancienne à la plus récente (c'est celle-là qu'on
relance), les créances soldées du règlement le plus récent au plus ancien.

Un élément de `content` :

```json
{
  "saleId": "uuid",
  "occurredAt": "2026-07-20T10:30:00",
  "customerId": "uuid",
  "customerGivenName": "Moussa",
  "customerFatherName": "Youssouf",
  "customerPhoneNumber": "+23566123456",
  "totalAmount":  { "amount": "200000.00", "currency": "XAF" },
  "amountPaid":   { "amount": "100000.00", "currency": "XAF" },
  "amountDue":    { "amount": "100000.00", "currency": "XAF" },
  "settled": false,
  "settledAt": null,
  "daysOutstanding": 12,
  "overdue": false
}
```

Sur une créance soldée, `settled` vaut `true`, `settledAt` porte la date du dernier encaissement,
et `amountDue.amount` vaut `"0.00"`.

### Détail d'une créance

`GET /api/v1/debts/{saleId}` → `200`

Reste accessible **après** règlement complet : c'est la preuve du paiement. Répond `404` avec
`code: "SALE_NOT_FOUND"` si la vente est au comptant — sans client, ce n'est pas une créance.

```json
{
  "saleId": "uuid",
  "occurredAt": "2026-07-20T10:30:00",
  "sellerId": "uuid",
  "sellerName": "Ahmat",
  "customerId": "uuid",
  "customerGivenName": "Moussa",
  "customerFatherName": "Youssouf",
  "customerPhoneNumber": "+23566123456",
  "lines": [
    {
      "productId": "uuid",
      "productName": "Plaquettes de frein",
      "productReference": "REF-42",
      "quantity": 3,
      "unitPrice": { "amount": "40000.00", "currency": "XAF" },
      "lineTotal": { "amount": "120000.00", "currency": "XAF" }
    }
  ],
  "totalAmount": { "amount": "200000.00", "currency": "XAF" },
  "amountPaid":  { "amount": "100000.00", "currency": "XAF" },
  "amountDue":   { "amount": "100000.00", "currency": "XAF" },
  "settled": false,
  "payments": [
    {
      "paymentId": "uuid",
      "amount": { "amount": "100000.00", "currency": "XAF" },
      "receivedAt": "2026-07-20T10:30:00",
      "receivedById": "uuid",
      "receivedByName": "Ahmat"
    }
  ]
}
```

`lines` arrive dans l'ordre du ticket remis au client. `payments` va du plus ancien au plus récent :
l'acompte versé le jour de la vente ouvre la liste, c'est un encaissement comme un autre — ne pas
le traiter à part.

`sellerName` et `receivedByName` peuvent valoir `null` si le compte a été supprimé. Afficher un
repli neutre (« Auteur non identifié »), jamais un vide.

### Historique des mouvements

`GET /api/v1/stock-movements` : chaque élément de `content` gagne trois champs.

```json
{
  "executedByName": "Ahmat",
  "operationId": "uuid",
  "saleAmountDue": { "amount": "50000.00", "currency": "XAF" }
}
```

`saleAmountDue` se lit en trois cas, et **un seul champ porte toute l'information** :

| Valeur | Signification | Affichage attendu |
|---|---|---|
| absent / `null` | le mouvement ne vient d'aucune vente (réception, transfert) | rien |
| `"0.00"` | vente réglée intégralement | `Payée` |
| positif | vente à crédit | `À crédit — reste 50 000 F CFA` |

Ne pas déduire l'état d'un autre champ, et ne pas ajouter de booléen local en doublon : deux
sources finiraient par se contredire.

---

## Règles sur les montants

Un montant est un objet `{ amount: string, currency: string }`. Le `amount` est une **chaîne**, pas
un nombre, pour éviter la perte de précision JavaScript sur les gros montants.

- Ne jamais recalculer un montant côté front. `amountDue`, `settled` et `daysOutstanding` viennent
  du serveur, qui est la seule source de vérité. Un total recalculé dans le navigateur finira par
  différer de celui du reçu papier, et c'est le front qu'on accusera.
- Ne pas convertir en `number` pour l'affichage. Formater à partir de la chaîne.
- Le franc CFA ne se manipule pas en centimes : le backend envoie `"50000.00"`, l'écran doit
  afficher `50 000 F CFA` — sans décimales, avec séparateur de milliers. Prévoir un formateur
  unique, réutilisé partout, plutôt qu'un `pipe` recopié par écran.

---

## Écran « Créances »

### En-tête

- titre `Créances` et une phrase courte : ce que les clients doivent encore au magasin ;
- un filtre à trois positions : `En cours` (défaut), `Soldées`, `Toutes` ;
- un total lisible du montant restant dû, visible uniquement sur `En cours` — la somme des dettes
  éteintes n'a aucun sens et laisserait croire à un encours qui n'existe pas.

Le filtre doit être dans l'URL (`?status=SETTLED`), pour qu'un rafraîchissement ou un partage de
lien retombe sur le même écran.

### Liste

Pour chaque créance :

- nom du client et téléphone ;
- date de la vente ;
- total, versé, et **restant dû** mis en avant ;
- ancienneté, formulée selon l'état :
  - créance en cours → `Ouverte depuis 12 jours`, et si `overdue` → `En retard` ;
  - créance soldée → `Réglée en 8 jours, le 28 juillet`, et si `overdue` → `Réglée hors délai`.

C'est le point le plus facile à rater. `daysOutstanding` s'arrête au jour du règlement pour une
créance éteinte : une dette réglée en une semaine il y a six mois affiche `8`, pas `180`. Et
`overdue` sur une créance soldée ne veut **pas** dire « en retard aujourd'hui » mais « a été réglée
au-delà du délai toléré ». Utiliser les mots de la seconde colonne, jamais ceux de la première.

Ne jamais représenter un état uniquement par une couleur : le retard, comme le règlement, doit se
lire en texte. Sur mobile, transformer chaque ligne en carte lisible sans défilement horizontal.

Chaque ligne ouvre le détail. La cible doit être un vrai lien navigable, pas un `click` sur une
`div` : le patron doit pouvoir ouvrir une créance dans un nouvel onglet.

### États vides

Ils diffèrent selon le filtre, et un message générique serait au mieux inutile :

- `En cours` vide → `Personne ne vous doit d'argent.` C'est une bonne nouvelle, la présenter comme
  telle plutôt qu'en état d'erreur.
- `Soldées` vide → `Aucune créance n'a encore été réglée.`
- `Toutes` vide → `Aucune vente à crédit n'a encore été enregistrée.`

---

## Écran « Détail d'une créance »

Route dédiée, atteignable depuis la liste et depuis la fiche client.

En-tête : le client, son téléphone, la date et l'heure de la vente, le vendeur. Puis, bien séparés :

1. **Ce qui a été vendu** — le tableau des lignes : produit nommé, référence, quantité, prix
   unitaire pratiqué ce jour-là, total de ligne. Préciser que le prix est celui du jour de la
   vente : c'est sur ce montant que la créance se discute, pas sur le tarif actuel.
2. **Où en est le règlement** — total, déjà versé, restant dû. Sur une créance soldée, remplacer le
   restant dû par `Soldée le 28 juillet 2026`.
3. **L'échéancier** — chaque encaissement : montant, date, et qui l'a reçu. Le premier est
   généralement l'acompte du comptoir ; ne pas le distinguer visuellement des suivants, c'est un
   versement comme un autre.

Un bouton `Enregistrer un versement` n'a de sens que si la créance est ouverte. Le masquer, pas le
désactiver silencieusement, quand `settled` vaut `true`.

Sur `404` / `SALE_NOT_FOUND` : message clair (`Cette vente n'est pas une créance.`) et retour vers
la liste, jamais une page blanche.

---

## Historique des mouvements

Ajouter sur chaque ligne issue d'une vente le repère décrit plus haut : `Payée`, ou
`À crédit — reste X`. Rien pour les réceptions et les transferts.

Quand le mouvement porte un `saleId` et que l'utilisateur est `OWNER`, la mention à crédit doit
mener au détail de la créance. C'est le chemin naturel : « cette sortie de stock, elle a été
payée ? » puis « montre-moi ».

Afficher `executedByName` à la place de l'identifiant de l'auteur, avec le même repli neutre en cas
de `null`.

---

## Fiche client

`GET /api/v1/customers/{customerId}/debts` accepte les mêmes `status`, `page` et `size`. Réutiliser
les mêmes composants de liste : c'est la même question posée sur un périmètre plus étroit, et deux
présentations différentes obligeraient le patron à se souvenir de quel écran il vient.

Deux sections : ce que le client doit encore, et ce qu'il a déjà réglé. La seconde répond à une
question réelle du commerçant — « est-ce que je peux encore lui faire crédit ? ».

---

## Chargements et erreurs

- squelette au premier affichage, pas de saut de mise en page ;
- chargement local sur l'action concernée, sans bloquer toute la page ;
- ne pas relancer une requête déjà en vol lorsqu'on change de filtre rapidement — annuler la
  précédente ;
- les erreurs suivent `ProblemDetail` et portent un champ `code`.

À traiter au minimum :

- `SALE_NOT_FOUND` → la vente n'est pas une créance, ou n'existe plus ;
- `401` → reprendre le parcours d'authentification ;
- `403` → retirer l'accès aux écrans de créances et revenir à un écran autorisé. Ne pas se contenter
  de cacher l'entrée de menu : un vendeur qui colle une URL doit atterrir proprement.

---

## Découpage attendu

- un service API typé, seul responsable des appels HTTP, avec des types alignés sur
  `api/openapi.yaml` du backend ;
- un store à base de signals par écran : liste (filtre, page, chargement) et détail ;
- des composants de présentation sans dépendance HTTP : ligne de créance, tableau des lignes de
  vente, échéancier, repère de règlement ;
- le repère `Payée / À crédit` est un composant partagé, utilisé par l'historique des mouvements et
  par la liste des créances — pas deux implémentations ;
- un formateur de montant unique pour toute l'application ;
- une garde d'autorisation `OWNER` sur les routes de créances ;
- aucun composant ne manipule directement les cookies d'authentification.

---

## Tests d'acceptation

- un vendeur ne voit pas l'entrée `Créances` et ne peut pas atteindre la route, même par URL ;
- le filtre par défaut affiche les créances en cours ;
- basculer sur `Soldées` affiche les créances éteintes, la plus récemment réglée en tête ;
- le filtre survit à un rafraîchissement de la page ;
- une créance soldée il y a six mois mais réglée en huit jours affiche `8 jours`, jamais `180` ;
- une créance réglée au-delà du délai affiche `Réglée hors délai`, pas `En retard` ;
- ouvrir une créance montre les produits nommés, les quantités, le vendeur, le versé et le restant ;
- l'échéancier liste tous les encaissements, acompte du comptoir compris ;
- le détail d'une créance reste consultable après règlement complet ;
- une vente au comptant ouverte par son identifiant affiche un message clair, pas une erreur brute ;
- l'historique des mouvements distingue `Payée`, `À crédit — reste X`, et rien pour un transfert ;
- aucun montant n'est recalculé côté front : couper le réseau après chargement ne change aucun
  chiffre affiché ;
- les états vides diffèrent selon le filtre ;
- les parcours sont utilisables au clavier, les dialogues rendent le focus à leur déclencheur, et
  tout état reste compréhensible sans couleur.

---

## Consignes de réalisation

Le backend est terminé et fusionné côté API ; le contrat fait foi et se trouve dans
`api/openapi.yaml` du dépôt backend.

- **Code propre partout.** Pas de composant qui appelle `HttpClient` directement, pas de logique
  métier dans un template, pas de `any`, pas de duplication entre l'écran global et la fiche
  client. Si une règle d'affichage apparaît deux fois, elle devient un composant ou une fonction
  partagée.
- **Respecter les conventions du dépôt** plutôt que d'en introduire : Angular 20, standalone,
  signals, `inject()`, aucune bibliothèque UI, tokens de `src/styles.scss` uniquement.
- **Écrire les tests** correspondant aux critères d'acceptation ci-dessus.
- **Faire les commits toi-même**, en français, au format du dépôt (`feat(...)`, `refactor(...)`,
  `test(...)`). Un commit par unité cohérente, pas un commit fourre-tout. Le message explique
  **pourquoi**, pas ce que le diff montre déjà.
- **Rédiger ensuite une description de PR complète** dans un fichier à la racine, couvrant le
  travail front de cette branche : ce que ça règle pour le commerçant, les décisions d'interface
  qui méritent un regard et pourquoi elles ont été tranchées ainsi, la rupture de contrat sur
  `/debts` et comment elle a été absorbée, et la couverture de tests. Elle doit se lire seule, sans
  connaître ce brief.
- **Ne pas pousser.** Laisser la branche prête à être poussée et fusionnée.
