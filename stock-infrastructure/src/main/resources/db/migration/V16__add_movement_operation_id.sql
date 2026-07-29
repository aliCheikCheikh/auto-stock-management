-- Regrouper les mouvements issus d'une même opération.
--
-- Une réception, un transfert ou une vente produisent plusieurs mouvements — un par produit,
-- parfois un par emplacement. L'historique les présentait comme autant d'événements indépendants :
-- rien ne distinguait trois lignes d'une même réception de trois réceptions séparées.
--
-- Regrouper d'après la date et l'auteur aurait été une heuristique : deux opérations rapprochées du
-- même vendeur auraient été fusionnées à tort. L'identité est donc posée explicitement à la source.

-- 1) Expand : colonne nullable, la table contient déjà des mouvements.
ALTER TABLE stock_movement ADD COLUMN operation_id UUID NULL;

-- 2) Reprise de l'existant.
--    Les sorties de stock d'une même vente forment déjà un groupe identifiable : on le conserve.
UPDATE stock_movement
SET operation_id = sale_id
WHERE sale_id IS NOT NULL
  AND operation_id IS NULL;

--    Pour les mouvements antérieurs sans vente (réceptions, transferts), aucun regroupement fiable
--    ne peut être reconstitué après coup : chacun devient sa propre opération. C'est exactement ce
--    que l'historique montrait jusqu'ici, donc aucune régression — seules les opérations futures
--    seront regroupées.
UPDATE stock_movement
SET operation_id = id
WHERE operation_id IS NULL;

-- 3) Contract : plus aucune ligne vide, la contrainte peut être posée.
ALTER TABLE stock_movement ALTER COLUMN operation_id SET NOT NULL;

-- Lecture de l'historique groupée par opération.
CREATE INDEX idx_stock_movement_operation_id ON stock_movement(operation_id);
