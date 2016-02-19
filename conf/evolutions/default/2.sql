# --- !Ups
ALTER TABLE entity_roles ADD CONSTRAINT unique_role UNIQUE (role_name);

# --- !Downs
ALTER TABLE entity_roles DROP CONSTRAINT IF EXISTS unique_role;