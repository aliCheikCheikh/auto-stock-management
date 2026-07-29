-- Encaissements successifs d'une vente : l'acompte du jour, puis les remboursements.
--
-- Jusqu'ici, une vente portait un unique montant encaissé (sale.amount_paid), figé à la création.
-- Un client revenant régler sa dette ne pouvait donc pas être enregistré. Le montant encaissé
-- devient la somme des paiements ; conserver en parallèle une colonne agrégée créerait deux
-- sources de vérité pour la même information.

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    sale_id UUID NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    -- Qui a encaissé : il s'agit d'argent, la traçabilité est une exigence.
    received_by UUID NOT NULL,
    CONSTRAINT fk_payment_sale
        FOREIGN KEY (sale_id) REFERENCES sale(id),
    CONSTRAINT fk_payment_received_by
        FOREIGN KEY (received_by) REFERENCES app_user(id),
    -- Un encaissement nul ou négatif n'a pas de sens : on n'enregistre pas un non-paiement,
    -- et un remboursement au client serait une autre opération, à modéliser explicitement.
    CONSTRAINT chk_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_payment_currency CHECK (length(trim(currency)) = 3)
);

-- Lecture des paiements d'une vente, et calcul du solde par agrégation.
CREATE INDEX idx_payment_sale_id ON payment(sale_id);

-- Reprise de l'existant : l'acompte déjà enregistré devient le premier paiement de chaque vente,
-- daté du jour de la vente et attribué au vendeur. Les ventes sans acompte n'en produisent aucun,
-- la contrainte exigeant un montant strictement positif.
INSERT INTO payment (id, sale_id, amount, currency, received_at, received_by)
SELECT gen_random_uuid(), s.id, s.amount_paid, s.amount_paid_currency, s.occurred_at, s.sold_by
FROM sale s
WHERE s.amount_paid > 0;

-- Les contraintes portant sur la colonne agrégée disparaissent avec elle.
--
-- Deux d'entre elles ne sont pas transposables : une contrainte CHECK ne peut pas traverser deux
-- tables, donc « l'encaissement ne dépasse pas le total » et « un solde dû implique un client » ne
-- peuvent plus être garantis en base. Ces règles restent portées par l'agrégat Sale et couvertes
-- par ses tests. C'est le prix de la suppression de la duplication, et il est assumé.
ALTER TABLE sale DROP CONSTRAINT IF EXISTS chk_sale_amount_paid_not_negative;
ALTER TABLE sale DROP CONSTRAINT IF EXISTS chk_sale_amount_paid_not_above_total;
ALTER TABLE sale DROP CONSTRAINT IF EXISTS chk_sale_amount_paid_currency;
ALTER TABLE sale DROP CONSTRAINT IF EXISTS chk_sale_credit_requires_customer;

ALTER TABLE sale DROP COLUMN amount_paid;
ALTER TABLE sale DROP COLUMN amount_paid_currency;
