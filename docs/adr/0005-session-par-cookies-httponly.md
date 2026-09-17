# 0005 — Session JWT dans des cookies HttpOnly

**Statut :** appliquée

## Contexte

L’interface Angular et l’API sont servies depuis la même origine (Nginx relaie `/api`). Un jeton stocké dans `localStorage` serait lisible par tout script injecté dans la page.

## Décision

- Jeton d’accès JWT valable 15 minutes, jeton de renouvellement valable 7 jours.
- Les deux sont transmis dans des cookies `HttpOnly` et `SameSite=Strict` ; l’attribut `Secure` est activé par configuration en production.
- Seule l’empreinte SHA-256 du jeton de renouvellement est stockée en base ; il peut être révoqué.
- Mots de passe hachés avec BCrypt ; un vendeur créé reçoit un mot de passe temporaire à changer à la première connexion.
- Les droits (propriétaire / vendeur) sont vérifiés par le serveur, indépendamment des gardes Angular.

## Conséquences

- Le code Angular ne manipule jamais le jeton ; un intercepteur renouvelle la session après un 401.
- La protection CSRF de Spring est désactivée et repose sur `SameSite=Strict` et la même origine. Ce choix est à réévaluer avant toute exposition sur plusieurs domaines.

## Dans le code

- [JwtService](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/security/JwtService.java), [RefreshTokenService](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/security/RefreshTokenService.java), [AuthController](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/web/controller/AuthController.java)
- [SecurityConfig](../../stock-infrastructure/src/main/java/com/aliCheikh/stock/infrastructure/security/SecurityConfig.java), [AuthFlowIntegrationTest](../../stock-infrastructure/src/test/java/com/aliCheikh/stock/infrastructure/web/controller/AuthFlowIntegrationTest.java)
