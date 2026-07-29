ALTER TABLE app_user
    ADD COLUMN display_name VARCHAR(100);

UPDATE app_user
SET display_name = trim(username)
WHERE display_name IS NULL;

ALTER TABLE app_user
    ALTER COLUMN display_name SET NOT NULL;

ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_display_name_not_blank
        CHECK (length(trim(display_name)) > 0);

-- Les tout premiers comptes précédaient l'authentification par email. Ils restent identifiables
-- sans inventer une adresse réelle, et pourront être renommés depuis l'administration.
UPDATE app_user
SET email = lower(regexp_replace(username, '[^a-zA-Z0-9]+', '-', 'g'))
            || '-' || left(id::text, 8) || '@legacy.local'
WHERE email IS NULL;
