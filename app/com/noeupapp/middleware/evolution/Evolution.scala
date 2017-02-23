package com.noeupapp.middleware.evolution


import play.api.mvc.{Action, Controller, Result}
import com.noeupapp.middleware.errorHandle.ExceptionEither._

import scala.concurrent.Future
import anorm._
import play.api.Logger

import scalaz.{-\/, \/-}
import scala.concurrent.ExecutionContext.Implicits.global

class Evolution extends Controller {

  def apply(id: Int) = Action.async {
    id match {
      case 0 => _init
      case _ => Future.successful(NotFound)
    }
  }

  private def applyHelper(sql: String): Future[Result] = {
    TryBDCall { implicit c =>
      \/-(SQL(sql).execute())
    } map {
      case -\/(e) =>
        Logger.error(e.toString)
        InternalServerError("InternalServerError")
      case \/-(_) => Ok("OK")
    }
  }

  def _init =
    applyHelper(
      """
        |CREATE TABLE middle_versions
        |(
        |  id               UUID            PRIMARY KEY NOT NULL,
        |  applied_at       TIMESTAMP       DEFAULT now() NOT NULL,
        |  applied_version  TEXT            NOT NULL,
        |  state            TEXT            NOT NULL default 'ok',
        |  last_problem     TEXT
        |);
        |
        |
        |CREATE TABLE entity_users
        |(
        |  id                UUID            PRIMARY KEY NOT NULL,
        |  first_name        TEXT,
        |  last_name         TEXT,
        |  email             TEXT,
        |  avatar_url        TEXT,
        |  active            BOOLEAN         DEFAULT FALSE,
        |  owned_by_client   TEXT,
        |  deleted           BOOLEAN         DEFAULT FALSE
        |);
        |
        |ALTER TABLE entity_users ADD created TIMESTAMP DEFAULT now() NOT NULL;
        |
        |
        |CREATE TABLE entity_login_infos (
        |  provider_id       VARCHAR(255) NOT NULL,
        |  provider_key      VARCHAR(255) NOT NULL,
        |  PRIMARY KEY (provider_id, provider_key)
        |);
        |
        |
        |CREATE TABLE entity_relation_login_infos_users (
        |  provider_id       VARCHAR(255) NOT NULL,
        |  provider_key      VARCHAR(255) NOT NULL,
        |  user_id           UUID         NOT NULL,
        |  PRIMARY KEY (provider_id, provider_key, user_id),
        |  FOREIGN KEY (provider_id, provider_key) REFERENCES entity_login_infos (provider_id, provider_key)
        |);
        |
        |
        |CREATE TABLE entity_roles (
        |  id                UUID          PRIMARY KEY NOT NULL,
        |  role_name         TEXT          NOT NULL
        |);
        |ALTER TABLE entity_roles ADD CONSTRAINT unique_role UNIQUE (role_name);
        |
        |
        |CREATE TABLE entity_relation_users_roles (
        |  role_id       UUID          NOT NULL REFERENCES entity_roles (id),
        |  user_id       UUID          NOT NULL REFERENCES entity_users (id),
        |  PRIMARY KEY (role_id, user_id)
        |);
        |
        |
        |CREATE TABLE auth_access_tokens
        |(
        |  token             TEXT            PRIMARY KEY NOT NULL,
        |  refresh_token     TEXT,
        |  client_id         TEXT            NOT NULL,
        |  user_uuid         UUID            NOT NULL,
        |  scope             TEXT,
        |  expires_in        BIGINT          DEFAULT 360000,
        |  created_at        TIMESTAMP       DEFAULT now() NOT NULL
        |);
        |
        |ALTER TABLE auth_access_tokens
        |  ADD CONSTRAINT auth_access_tokens_entity_users_id_fk
        |  FOREIGN KEY (user_uuid) REFERENCES entity_users (id);
        |
        |
        |CREATE TABLE auth_auth_codes
        |(
        |  authorization_code TEXT           PRIMARY KEY NOT NULL,
        |  user_uuid         UUID            NOT NULL,
        |  redirect_uri      TEXT,
        |  created_at        TIMESTAMP       NOT NULL,
        |  client_id         TEXT            NOT NULL,
        |  expires_in        INTEGER         NOT NULL,
        |  used              BOOLEAN
        |);
        |
        |CREATE TABLE auth_client_grant_types
        |(
        |  client_id         TEXT            NOT NULL,
        |  grant_type        TEXT            NOT NULL,
        |  PRIMARY KEY (grant_type, client_id)
        |);
        |
        |CREATE TABLE auth_clients
        |(
        |  client_id         TEXT            PRIMARY KEY NOT NULL,
        |  client_name       TEXT            NOT NULL,
        |  client_secret     TEXT            NOT NULL,
        |  description       TEXT            NOT NULL,
        |  redirect_uri      TEXT            NOT NULL,
        |  scope             TEXT            NOT NULL
        |);
        |
        |
        |ALTER TABLE entity_users
        |  ADD CONSTRAINT entity_users_auth_clients_client_id_fk
        |  FOREIGN KEY (owned_by_client) REFERENCES auth_clients (client_id);
        |
        |ALTER TABLE auth_access_tokens
        |  ADD CONSTRAINT auth_access_tokens_auth_clients_client_id_fk
        |  FOREIGN KEY (client_id) REFERENCES auth_clients (client_id);
        |
        |CREATE TABLE entity_organisations
        |(
        |  id                UUID            PRIMARY KEY NOT NULL,
        |  name              TEXT            NOT NULL,
        |  subdomain         TEXT            NOT NULL,
        |  logo_url          TEXT,
        |  color             TEXT,
        |  credits           BIGINT          NOT NULL DEFAULT 5,
        |  created           TIMESTAMP       DEFAULT now() NOT NULL,
        |  deleted           BOOLEAN         DEFAULT FALSE
        |);
        |
        |
        |CREATE TABLE entity_groups
        |(
        |  id               UUID            PRIMARY KEY NOT NULL,
        |  name             TEXT            NOT NULL,
        |  owner            UUID,
        |  deleted          BOOLEAN         NOT NULL DEFAULT FALSE
        |);
        |
        |
        |CREATE TABLE entity_entities
        |(
        |  id                UUID            PRIMARY KEY NOT NULL,
        |  type              TEXT            NOT NULL,
        |  parent            UUID,
        |  account_type      TEXT            DEFAULT 'standard'
        |);
        |ALTER TABLE entity_entities ADD CONSTRAINT entity_entitiesfk1 FOREIGN KEY (parent) REFERENCES entity_entities (id);
        |
        |
        |CREATE TABLE entity_properties
        |(
        |  id                BIGSERIAL       PRIMARY KEY NOT NULL,
        |  entity            UUID            NOT NULL,
        |  key               TEXT            NOT NULL,
        |  value             TEXT            DEFAULT 'standard',
        |  type              TEXT            DEFAULT 'text',
        |  expire            TIMESTAMP,
        |  deleted           BOOLEAN         DEFAULT FALSE NOT NULL
        |);
        |ALTER TABLE entity_properties
        |  ADD CONSTRAINT entity_propertiesfk3
        |  FOREIGN KEY (entity) REFERENCES entity_entities (id);
        |
        |
        |-- make sure that foreign key entities does correspond to the right type (ie an organization does not refers to user!)
        |CREATE FUNCTION check_correct_type() RETURNS trigger AS $$
        |DECLARE
        |entity_type text;
        |
        |BEGIN
        |
        |  SELECT type
        |  INTO entity_type
        |  FROM entity_entities
        |  WHERE id = NEW.id;
        |--AND type <> TG_ARGV [0]
        |
        |-- check whether 'type' refers to the right table type
        |IF (entity_type <> TG_ARGV [0])
        |THEN
        |  RAISE EXCEPTION 'Entity already exists with another type';
        |END IF;
        |
        |-- insert new record in entities if none found
        |IF (entity_type IS NULL)
        |THEN
        |  INSERT INTO entity_entities (id, type) VALUES (NEW.id, TG_ARGV [0]);
        |END IF;
        |
        |-- return new record
        |RETURN NEW;
        |END;
        |$$ LANGUAGE plpgsql;
        |
        |
        |CREATE TRIGGER check_correct_type_user BEFORE INSERT OR UPDATE ON entity_users
        |FOR EACH ROW EXECUTE PROCEDURE check_correct_type('user');
        |
        |CREATE TRIGGER check_correct_type_group BEFORE INSERT OR UPDATE ON entity_groups
        |FOR EACH ROW EXECUTE PROCEDURE check_correct_type('group');
        |
        |CREATE TRIGGER check_correct_type_orga BEFORE INSERT OR UPDATE ON entity_organisations
        |FOR EACH ROW EXECUTE PROCEDURE check_correct_type('orga');
        |
        |
        |CREATE TABLE pass_hashes
        |(
        |  "user" UUID PRIMARY KEY,
        |  hasher TEXT NOT NULL,
        |  hash TEXT NOT NULL,
        |  salt TEXT,
        |  last_modified TIMESTAMP DEFAULT now() NOT NULL,
        |  CONSTRAINT pass_hashes_entity_users_id_fk FOREIGN KEY ("user") REFERENCES entity_users (id)
        |);
        |
        |-- insert default values
        |INSERT INTO entity_organisations (id, name, subdomain)
        |VALUES (
        |  '30000000-0000-0000-0000-000000000000',
        |  'Master app organisation',
        |  'myapp'
        |);
        |
        |INSERT INTO entity_groups (id, name)
        |VALUES (
        |  '20000000-0000-0000-0000-000000000000',
        |  'Administrators'
        |);
        |
        |INSERT INTO entity_roles (id, role_name)
        |VALUES (
        |  '50000000-0000-0000-0000-000000000000',
        |  'superadmin'
        |);
        |
        |
        |INSERT INTO middle_versions (id, applied_version)
        |VALUES (
        |  '40000000-0000-0000-0000-000000000000',
        |  'initial setup'
        |);
      """
        .stripMargin)
}