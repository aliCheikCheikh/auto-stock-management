-- Le patron gère désormais ses propres familles de pièces.
--
-- La contrainte UNIQUE posée en V1 est sensible à la casse : « Freinage » et « freinage »
-- pouvaient coexister, soit deux familles pour la base et une seule dans la tête du patron.
-- Un index unique sur le nom normalisé exprime la règle réellement voulue.
--
-- L'ancienne contrainte est conservée : elle reste vraie et ne coûte rien.

CREATE UNIQUE INDEX uk_category_name_lower ON category (LOWER(TRIM(name)));
