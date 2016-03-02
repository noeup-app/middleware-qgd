# --- !Ups

CREATE TABLE entity_users
(
  id                UUID            PRIMARY KEY NOT NULL,
  first_name        TEXT,
  last_name         TEXT,
  email             TEXT,
  avatar_url        TEXT,
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


-- Creates auth tables

CREATE TABLE auth_access_tokens
(
  token             TEXT            PRIMARY KEY NOT NULL,
  refresh_token     TEXT,
  client_id         UUID            NOT NULL,
  user_uuid         UUID            NOT NULL,
  scope             TEXT,
  expires_in        BIGINT          DEFAULT 360000,
  created_at        TIMESTAMP       DEFAULT now() NOT NULL
);


--CREATE TABLE auth_grant_types
--(
--  id                BIGSERIAL       PRIMARY KEY NOT NULL,
--  grant_type        TEXT            NOT NULL
--);

CREATE TABLE auth_auth_codes
(
  authorization_code TEXT           PRIMARY KEY NOT NULL,
  user_uuid         UUID            NOT NULL,
  redirect_uri      TEXT,
  created_at        TIMESTAMP       NOT NULL,
  client_id         UUID            NOT NULL,
  expires_in        INTEGER         NOT NULL,
  used              BOOLEAN
);

CREATE TABLE auth_client_grant_types
(
  client_id         UUID            NOT NULL,
  grant_type        TEXT            NOT NULL,
  PRIMARY KEY (grant_type, client_id)
);

CREATE TABLE auth_clients
(
  client_id         UUID            PRIMARY KEY NOT NULL,
  client_name       TEXT            NOT NULL,
  client_secret     TEXT            NOT NULL,
  description       TEXT            NOT NULL,
  redirect_uri      TEXT            NOT NULL,
  scope             TEXT            NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS entity_users CASCADE;
DROP TABLE IF EXISTS entity_login_infos CASCADE;
DROP TABLE IF EXISTS entity_roles CASCADE;
DROP TABLE IF EXISTS entity_relation_login_infos_users CASCADE;
DROP TABLE IF EXISTS entity_relation_users_roles CASCADE;

DROP TABLE IF EXISTS auth_access_tokens CASCADE;
--DROP TABLE IF EXISTS auth_grant_types CASCADE;
DROP TABLE IF EXISTS auth_auth_codes CASCADE;
DROP TABLE IF EXISTS auth_client_grant_types CASCADE;
DROP TABLE IF EXISTS auth_clients CASCADE;

