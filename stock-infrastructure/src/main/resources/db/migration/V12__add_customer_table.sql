-- Créances (ventes à crédit) : le client débiteur et le montant encaissé à la vente.
-- Migration strictement additive : aucune colonne ni table existante n'est supprimée ou renommée.

CREATE TABLE customer (
    id UUID PRIMARY KEY,
    given_name VARCHAR(100) NOT NULL,
    father_name VARCHAR(100) NULL,
    -- Forme canonique E.164 (+235XXXXXXXX), garantie par le Value Object PhoneNumber.
    -- UNIQUE : un numéro identifie un et un seul client.
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    -- UNIQUE tolère plusieurs NULL (les NULL sont distincts entre eux en PostgreSQL),
    -- ce qui donne exactement la règle voulue : « unique seulement s'il est renseigné ».
    email VARCHAR(200) NULL UNIQUE,
    -- Date d'enregistrement : sert à proposer les derniers clients dans le sélecteur de vente,
    -- et donne une trace d'audit sur l'ouverture d'un compte à crédit.
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (length(trim(given_name)) > 0),
    CHECK (father_name IS NULL OR length(trim(father_name)) > 0),
    CHECK (email IS NULL OR length(trim(email)) > 0)
);

-- Client débiteur : NULL pour une vente au comptant, renseigné dès qu'un solde reste dû.
ALTER TABLE sale ADD COLUMN customer_id UUID NULL;

ALTER TABLE sale
    ADD CONSTRAINT fk_sale_customer
    FOREIGN KEY (customer_id) REFERENCES customer(id);

-- Recherche des créances par client (sans index, « toutes les dettes de X » scannerait la table).
CREATE INDEX idx_sale_customer_id ON sale(customer_id);

-- Montant encaissé au moment de la vente.
--
-- On ne peut PAS créer ces colonnes directement en NOT NULL : la table contient déjà des ventes,
-- pour lesquelles PostgreSQL n'aurait aucune valeur à écrire et refuserait toute la migration.
-- Un DEFAULT ne conviendrait pas non plus : la valeur de remplissage n'est pas une constante,
-- elle dépend de chaque ligne (le total de CETTE vente).
-- D'où la séquence en trois temps : ajouter nullable, remplir, puis durcir.

-- 1) Expand : colonnes nullables, acceptées quel que soit le contenu de la table.
ALTER TABLE sale ADD COLUMN amount_paid NUMERIC(15, 2) NULL;
ALTER TABLE sale ADD COLUMN amount_paid_currency CHAR(3) NULL;

-- 2) Backfill : les ventes antérieures aux créances ont toutes été payées comptant,
--    leur montant encaissé vaut donc leur total.
UPDATE sale
SET amount_paid = total_amount,
    amount_paid_currency = total_currency
WHERE amount_paid IS NULL;

-- 3) Contract : plus aucune ligne vide, la contrainte peut être posée.
ALTER TABLE sale ALTER COLUMN amount_paid SET NOT NULL;
ALTER TABLE sale ALTER COLUMN amount_paid_currency SET NOT NULL;

-- La base se défend elle-même, indépendamment des règles du domaine.
ALTER TABLE sale ADD CONSTRAINT chk_sale_amount_paid_not_negative CHECK (amount_paid >= 0);
ALTER TABLE sale ADD CONSTRAINT chk_sale_amount_paid_not_above_total CHECK (amount_paid <= total_amount);
ALTER TABLE sale ADD CONSTRAINT chk_sale_amount_paid_currency CHECK (amount_paid_currency = total_currency);
-- Pas de solde dû sans client : traduction en base de l'invariant de l'agrégat Sale.
ALTER TABLE sale ADD CONSTRAINT chk_sale_credit_requires_customer
    CHECK (amount_paid = total_amount OR customer_id IS NOT NULL);
