# --- !Ups
ALTER TABLE auth_clients
  ALTER COLUMN client_id SET DATA TYPE TEXT
  USING client_id :: TEXT
  
ALTER TABLE auth_auth_codes
  ALTER COLUMN client_id SET DATA TYPE TEXT
  USING client_id :: TEXT

ALTER TABLE auth_client_grant_types
  ALTER COLUMN client_id SET DATA TYPE TEXT
  USING client_id :: TEXT

# --- !Downs
ALTER TABLE auth_clients
  ALTER COLUMN client_id SET DATA TYPE UUID
  USING client_id :: UUID

ALTER TABLE auth_auth_codes
  ALTER COLUMN client_id SET DATA TYPE UUID
  USING client_id :: UUID

ALTER TABLE auth_client_grant_types
  ALTER COLUMN client_id SET DATA TYPE UUID
  USING client_id :: UUID

