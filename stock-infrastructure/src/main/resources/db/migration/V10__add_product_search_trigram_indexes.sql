CREATE
EXTENSION IF NOT EXISTS pg_trgm;
CREATE
EXTENSION IF NOT EXISTS unaccent;

CREATE FUNCTION f_unaccent(text)
    RETURNS text
    LANGUAGE sql IMMUTABLE
AS $$
SELECT public.unaccent('public.unaccent'::regdictionary, $1)
           $$;

CREATE INDEX idx_product_name_trgm ON product USING gin (f_unaccent(lower (name)) gin_trgm_ops);
CREATE INDEX idx_product_reference_trgm ON product USING gin (f_unaccent(lower (reference)) gin_trgm_ops);
