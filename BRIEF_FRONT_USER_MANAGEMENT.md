# Brief front — Gestion des utilisateurs

## Objectif

Remplacer l'administration rudimentaire par un écran clair, sûr et utilisable dans la boutique.
Seul un utilisateur `OWNER` peut ouvrir cet écran. Un vendeur ne doit ni voir l'entrée de
navigation ni pouvoir atteindre la route.

Contraintes du projet : Angular 20, composants standalone, signals, `inject()`, aucune bibliothèque
UI et réutilisation exclusive des design tokens de `src/styles.scss`.

## Contrat API

Toutes les requêtes utilisent les cookies d'authentification existants.

### Lister

`GET /api/v1/users` → `200`

```json
[
  {
    "userId": "uuid",
    "displayName": "Amina Mahamat",
    "email": "amina@example.com",
    "role": "SELLER",
    "passwordChangeRequired": true,
    "active": true
  }
]
```

### Créer un vendeur

`POST /api/v1/users` avec :

```json
{
  "displayName": "Amina Mahamat",
  "email": "amina@example.com"
}
```

Réponse `201`, avec en-tête `Cache-Control: no-store` :

```json
{
  "user": {
    "userId": "uuid",
    "displayName": "Amina Mahamat",
    "email": "amina@example.com",
    "role": "SELLER",
    "passwordChangeRequired": true,
    "active": true
  },
  "temporaryPassword": "valeur-affichée-une-seule-fois"
}
```

### Renommer

`PATCH /api/v1/users/{userId}/display-name` avec
`{"displayName":"Nouveau nom"}` → `200` et l'utilisateur mis à jour.

### Désactiver

`DELETE /api/v1/users/{userId}` → `204`. Il s'agit d'une désactivation, jamais d'une suppression
définitive.

### Réactiver

`POST /api/v1/users/{userId}/reactivate` sans corps → `200` et l'utilisateur mis à jour.

### Réinitialiser le mot de passe d'un vendeur

`POST /api/v1/users/{userId}/reset-password` sans corps → `200`, avec
`Cache-Control: no-store` :

```json
{
  "temporaryPassword": "valeur-affichée-une-seule-fois"
}
```

Cette action est interdite pour un `OWNER`.

### Changer son mot de passe temporaire

`POST /api/v1/auth/change-password` avec :

```json
{
  "currentPassword": "mot-de-passe-temporaire",
  "newPassword": "nouveau-mot-de-passe"
}
```

Réponse `204`. Le nouveau mot de passe contient entre 8 et 72 caractères.

`POST /api/v1/auth/login` et `GET /api/v1/auth/me` renvoient notamment `displayName`, `role` et
`passwordTemporary`. Tant que `passwordTemporary` vaut `true`, toutes les fonctions métier sont
bloquées côté backend, sauf le changement de mot de passe et la déconnexion.

## Écran d'administration

### En-tête

- titre `Utilisateurs` et texte court expliquant que les comptes donnent accès aux données du
  magasin ;
- bouton principal `Ajouter un vendeur` ;
- compteur lisible : total, actifs et désactivés ;
- recherche locale par nom ou email si la liste contient plusieurs utilisateurs.

### Liste

Afficher pour chaque utilisateur :

- nom affiché, email et repère `Vous` pour le compte connecté ;
- rôle traduit (`Propriétaire` ou `Vendeur`) ;
- état explicite (`Actif` ou `Désactivé`) ;
- indicateur `Changement de mot de passe en attente` lorsque nécessaire ;
- menu d'actions adapté à l'état et au rôle.

Ne jamais représenter l'état uniquement par une couleur. Sur mobile, transformer chaque ligne en
carte lisible sans défilement horizontal.

Actions disponibles :

- `Renommer` pour tout utilisateur ;
- `Désactiver` uniquement pour un compte actif ;
- `Réactiver` uniquement pour un compte désactivé ;
- `Réinitialiser le mot de passe` uniquement pour un vendeur.

Ne jamais proposer la réinitialisation d'un propriétaire. La procédure de secours reste hors de
l'interface web.

## Parcours et confirmations

### Ajout

Ouvrir un dialogue avec `Nom affiché` et `Email de connexion`. Valider les champs avant l'envoi,
mais conserver les erreurs retournées par l'API comme source de vérité.

Après la création, remplacer le formulaire par un dialogue non ambigu :

- afficher le mot de passe temporaire une seule fois ;
- fournir un bouton `Copier` avec confirmation visuelle ;
- expliquer qu'il faut le transmettre oralement et qu'il ne pourra pas être retrouvé ;
- ne jamais placer cette valeur dans un toast, une URL, le stockage navigateur, les journaux ou un
  outil de suivi ;
- vider la valeur de l'état du composant à la fermeture du dialogue.

### Renommage

Préremplir le nom actuel. Le nom est obligatoire, débarrassé de ses espaces de bord et limité à
100 caractères. Mettre à jour la ligne uniquement après la réponse `200`.

### Désactivation

Demander une confirmation mentionnant le nom et l'email. Expliquer que la personne perd
immédiatement l'accès mais que son historique est conservé. Le bouton destructif porte le libellé
`Désactiver`, jamais `Supprimer`.

Le front peut désactiver visuellement l'action sur l'unique propriétaire actif, mais il doit toujours
traiter l'erreur backend `LAST_ACTIVE_OWNER` : `Le magasin doit toujours conserver au moins un
propriétaire actif.`

### Réinitialisation vendeur

Demander confirmation en précisant que les sessions existantes seront fermées et que le vendeur
devra choisir un nouveau mot de passe. Après succès, utiliser le même dialogue secret à affichage
unique que lors de la création.

### Changement forcé à la connexion

Ajouter une garde globale : si `passwordTemporary` vaut `true`, rediriger vers un écran dédié
qui ne contient que le changement de mot de passe et la déconnexion. Ne pas afficher la navigation
métier derrière cet écran. Après le `204`, recharger `/api/v1/auth/me`, puis rediriger vers l'accueil.

## Chargements et erreurs

- squelette ou état de chargement au premier affichage ;
- état vide utile avec bouton d'ajout si aucun utilisateur n'est affiché ;
- chargement local sur l'action concernée, sans bloquer toute la page ;
- empêcher les doubles soumissions ;
- conserver le formulaire ouvert si l'API refuse la demande ;
- toast uniquement pour confirmer une action terminée ; les erreurs de champ restent au voisinage
  des champs.

Les erreurs suivent `ProblemDetail` et contiennent un champ `code`. Traiter au minimum :

- `USER_EMAIL_ALREADY_USED` → email déjà attribué ;
- `USER_NOT_FOUND` → compte supprimé ou modifié ailleurs, puis recharger la liste ;
- `LAST_ACTIVE_OWNER` → désactivation impossible ;
- `OWNER_PASSWORD_RESET_FORBIDDEN` → action indisponible pour un propriétaire ;
- `CURRENT_PASSWORD_INCORRECT` → erreur sur le mot de passe actuel ;
- `INVALID_USER_DATA` et les erreurs de validation → message précis près du champ ;
- `401` → reprendre le parcours d'authentification ;
- `403` → retirer l'accès à l'administration et revenir à un écran autorisé.

## Découpage attendu

Garder les responsabilités séparées :

- un service API typé, seul responsable des appels HTTP ;
- un store/service de page à base de signals pour la liste, les chargements et les mutations ;
- des composants de présentation pour la liste, le formulaire, la confirmation et le secret à
  usage unique ;
- une garde d'autorisation `OWNER` distincte de la garde de changement de mot de passe ;
- aucun composant ne manipule directement les cookies d'authentification.

## Tests d'acceptation

- un vendeur ne voit pas et ne peut pas ouvrir l'administration ;
- la liste distingue clairement rôles, états et changement de mot de passe en attente ;
- créer un vendeur affiche son secret une fois, puis l'efface à la fermeture ;
- un email dupliqué reste dans le formulaire avec une erreur compréhensible ;
- renommer met à jour le nom sans modifier l'email ;
- désactiver exige une confirmation et traite `LAST_ACTIVE_OWNER` ;
- réactiver restaure l'état actif ;
- la réinitialisation n'est jamais proposée pour un propriétaire ;
- un vendeur connecté avec un mot de passe temporaire ne peut atteindre aucun écran métier ;
- après changement du mot de passe, l'accès normal est restauré ;
- les parcours sont utilisables au clavier, les dialogues rendent le focus à leur déclencheur et
  les libellés restent compréhensibles sans couleur.
