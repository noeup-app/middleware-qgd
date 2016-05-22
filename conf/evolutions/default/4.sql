# --- !Ups

--  Creates entities tables
CREATE TABLE IF NOT EXISTS entity_organisations
(
  id                UUID            PRIMARY KEY NOT NULL,
  name              TEXT            NOT NULL,
  subdomain         TEXT            NOT NULL,
  logo_url          TEXT,
  color             TEXT,
  credits           BIGINT          NOT NULL DEFAULT 5,
  deleted           BOOLEAN         DEFAULT FALSE
);

-- TODO add these columns in other tables
-- ALTER TABLE entity_users ADD COLUMN
--   pin                  INTEGER;
--
-- ALTER TABLE entity_users ADD COLUMN
--   base_node             UUID          NOT NULL;

CREATE TABLE IF NOT EXISTS entity_groups
(
  id               UUID            PRIMARY KEY NOT NULL,
  name             TEXT            NOT NULL,
  owner            UUID,
  deleted          BOOLEAN         NOT NULL DEFAULT FALSE
);


CREATE TABLE IF NOT EXISTS entity_entities
(
  id                UUID            PRIMARY KEY NOT NULL,
  type              TEXT            NOT NULL,
  parent            UUID,
  account_type      TEXT            DEFAULT 'standard'
);

CREATE TABLE IF NOT EXISTS entity_properties
(
  id                BIGSERIAL       PRIMARY KEY NOT NULL,
  entity            UUID            NOT NULL,
  key               TEXT            NOT NULL,
  value             TEXT            DEFAULT 'standard',
  type              TEXT            DEFAULT 'text',
  expire            TIMESTAMP,
  deleted           BOOLEAN         DEFAULT FALSE NOT NULL
);

-- Creates entity constraints

-- ALTER TABLE entity_users ADD CONSTRAINT basenodefk1 FOREIGN KEY (base_node) REFERENCES  instance_document_instances(id);

ALTER TABLE entity_entities DROP CONSTRAINT IF EXISTS parentfk1;
ALTER TABLE entity_entities ADD CONSTRAINT parentfk1 FOREIGN KEY (parent) REFERENCES entity_entities (id);

ALTER TABLE entity_properties DROP CONSTRAINT IF EXISTS entityfk3;
ALTER TABLE entity_properties ADD CONSTRAINT entityfk3 FOREIGN KEY (entity) REFERENCES entity_entities (id);


-- make sure that foreign key entities does correspond to the right type (ie an organization does not refers to user!)
DROP FUNCTION IF EXISTS check_correct_type();
CREATE FUNCTION check_correct_type() RETURNS trigger AS $$
DECLARE
entity_type text;;

BEGIN

  SELECT type
  INTO entity_type
  FROM entity_entities
  WHERE id = NEW.id;;
--AND type <> TG_ARGV [0]

-- check whether 'type' refers to the right table type
IF (entity_type <> TG_ARGV [0])
THEN
  RAISE EXCEPTION 'Entity already exists with another type';;
END IF;;

-- insert new record in entities if none found
IF (entity_type IS NULL)
THEN
  INSERT INTO entity_entities (id, type) VALUES (NEW.id, TG_ARGV [0]);;
END IF;;

-- return new record
RETURN NEW;;
END;;
$$ LANGUAGE plpgsql;


CREATE TRIGGER check_correct_type_user BEFORE INSERT OR UPDATE ON entity_users
FOR EACH ROW EXECUTE PROCEDURE check_correct_type('user');

CREATE TRIGGER check_correct_type_group BEFORE INSERT OR UPDATE ON entity_groups
FOR EACH ROW EXECUTE PROCEDURE check_correct_type('group');

CREATE TRIGGER check_correct_type_orga BEFORE INSERT OR UPDATE ON entity_organisations
FOR EACH ROW EXECUTE PROCEDURE check_correct_type('orga');

-- DELETE FROM entity_users WHERE id =  '10000000-0000-0000-0000-000000000000';
-- DELETE FROM entity_groups WHERE id = '20000000-0000-0000-0000-000000000000';
-- DELETE FROM entity_organisations WHERE id = '30000000-0000-0000-0000-000000000000';
-- DELETE FROM entity_users WHERE id = '30000000-0000-0000-0000-000000000000';
-- DELETE FROM instance_document_instances WHERE id = '00000000-0000-0000-0000-000000000000';

-- INSERT INTO instance_document_instances (id, name, instance, document, parent, creator, prefix, created, deleted)
-- VALUES ('00000000-0000-0000-0000-000000000000', 'super_root', null, null, null, null, null, DEFAULT, DEFAULT);


INSERT INTO entity_organisations (id, name, subdomain)
VALUES (
  '30000000-0000-0000-0000-000000000000',
  'orga super',
  'subdomorga'
);

INSERT INTO entity_groups (id, name, owner)
VALUES (
  '20000000-0000-0000-0000-000000000000',
  'group super',
  '10000000-0000-0000-0000-000000000000'
);


ALTER TABLE public.auth_access_tokens ALTER COLUMN client_id TYPE TEXT USING client_id::TEXT;

# --- !Downs

-- Drop triggers and functions

DROP FUNCTION IF EXISTS check_correct_type() CASCADE;

-- Drop table CASCADEs

DROP TABLE IF EXISTS entity_organisations CASCADE;

-- ALTER TABLE entity_users DROP COLUMN pin;
--
-- ALTER TABLE entity_users DROP COLUMN base_node;

DROP TABLE IF EXISTS entity_groups CASCADE;
DROP TABLE IF EXISTS entity_relation_users_groups CASCADE;
DROP TABLE IF EXISTS entity_entities CASCADE;
DROP TABLE IF EXISTS entity_properties CASCADE;



