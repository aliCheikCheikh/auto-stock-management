INSERT INTO category (id, name) VALUES
    (gen_random_uuid(), 'Freinage'),
    (gen_random_uuid(), 'Embrayage'),
    (gen_random_uuid(), 'Filtration'),
    (gen_random_uuid(), 'Moteur'),
    (gen_random_uuid(), 'Transmission'),
    (gen_random_uuid(), 'Suspension'),
    (gen_random_uuid(), 'Direction'),
    (gen_random_uuid(), 'Electrique'),
    (gen_random_uuid(), 'Pneumatique'),
    (gen_random_uuid(), 'Refroidissement'),
    (gen_random_uuid(), 'Echappement'),
    (gen_random_uuid(), 'Hydraulique')
ON CONFLICT (name) DO NOTHING;
