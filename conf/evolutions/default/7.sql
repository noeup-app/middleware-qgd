# --- !Ups
ALTER TABLE public.auth_access_tokens ALTER COLUMN client_id TYPE TEXT USING client_id::TEXT;

# --- !Downs

