package com.noeupapp.middleware.utils.slick

import java.sql.JDBCType
import java.util.UUID

import com.github.tminglei.slickpg.json.PgJsonExtensions
import play.api.libs.json.{JsValue, Json}

import com.github.tminglei.slickpg._
import slick.driver.JdbcProfile
import slick.jdbc.{PositionedParameters, SetParameter, PositionedResult}
import slick.profile.Capability

trait MyPostgresDriver extends ExPostgresDriver
  with PgArraySupport
  with PgDateSupport
  with PgDateSupportJoda
  with PgRangeSupport
  with PgHStoreSupport
  with PgPlayJsonSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {
  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcProfile.capabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with DateTimeImplicits
    with JodaDateTimePlainImplicits
//    with JodaDateTimeImplicits
    with JsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with UUIDPlainImplicits
    with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        (s) => utils.SimpleArrayUtils.fromString[JsValue](Json.parse(_))(s).orNull,
        (v) => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)
  }
}

trait UUIDPlainImplicits {

  implicit class PgPositionedResult(val r: PositionedResult) {
    def nextUUID: UUID = UUID.fromString(r.nextString)

    def nextUUIDOption: Option[UUID] = r.nextStringOption().map(UUID.fromString)
  }

  implicit object SetUUID extends SetParameter[UUID] {
    def apply(v: UUID, pp: PositionedParameters) {
      pp.setObject(v, JDBCType.BINARY.getVendorTypeNumber)
    }
  }

}

object MyPostgresDriver extends MyPostgresDriver

