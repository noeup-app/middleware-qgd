# --- !Ups

CREATE TABLE entity_users
(
  id                UUID            PRIMARY KEY NOT NULL,
  first_name        TEXT,
  last_name         TEXT,
  email             TEXT,
  avatar_url        TEXT,
  pin               INTEGER,
  base_node         UUID            NOT NULL,
  active            BOOLEAN         DEFAULT FALSE,
  deleted           BOOLEAN         DEFAULT FALSE
);


CREATE TABLE entity_login_infos (
  provider_id       VARCHAR(255) NOT NULL,
  provider_key      VARCHAR(255) NOT NULL,
  PRIMARY KEY (provider_id, provider_key)
);


CREATE TABLE entity_relation_login_infos_users (
  provider_id       VARCHAR(255) NOT NULL,
  provider_key      VARCHAR(255) NOT NULL,
  user_id           UUID         NOT NULL,
  PRIMARY KEY (provider_id, provider_key, user_id),
  FOREIGN KEY (provider_id, provider_key) REFERENCES entity_login_infos (provider_id, provider_key)
);


CREATE TABLE entity_roles (
  id                UUID          PRIMARY KEY NOT NULL,
  role_name         TEXT          NOT NULL
);

CREATE TABLE entity_relation_users_roles (
  role_id       UUID          NOT NULL REFERENCES entity_roles (id),
  user_id       UUID          NOT NULL REFERENCES entity_users (id),
  PRIMARY KEY (role_id, user_id)
);

# --- !Downs

DROP TABLE IF EXISTS entity_users CASCADE;
DROP TABLE IF EXISTS entity_login_infos CASCADE;
DROP TABLE IF EXISTS entity_roles CASCADE;
DROP TABLE IF EXISTS entity_relation_login_infos_users CASCADE;
DROP TABLE IF EXISTS entity_relation_users_roles CASCADE;

