# --- !Ups

CREATE TABLE entity_users
(
  id                UUID            PRIMARY KEY NOT NULL,
  login             TEXT            NOT NULL,
  password          TEXT            NOT NULL,
  first_name        TEXT,
  last_name         TEXT,
  pin               INTEGER,
  base_node         UUID            NOT NULL,
  active            BOOLEAN         DEFAULT FALSE,
  deleted           BOOLEAN         DEFAULT FALSE
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

-- Drop table CASCADEs

DROP TABLE IF EXISTS entity_users CASCADE;
DROP TABLE IF EXISTS auth_access_tokens CASCADE;
--DROP TABLE IF EXISTS auth_grant_types CASCADE;
DROP TABLE IF EXISTS auth_auth_codes CASCADE;
DROP TABLE IF EXISTS auth_client_grant_types CASCADE;
DROP TABLE IF EXISTS auth_clients CASCADE;

