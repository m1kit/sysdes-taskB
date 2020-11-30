package models

import java.sql.Timestamp

import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}
import play.api.libs.json.Reads._

/**
  * Domain model of a user
  * @param id         ID
  * @param name       名前
  * @param password   パスワード
  * @param createdAt  作成日時
  */
case class Credential(id: Int, name: String, password: String, createdAt: Timestamp)

object Credential extends DomainModel[Credential] {
  import slick.jdbc.GetResult
  implicit def getResult: GetResult[Credential] = GetResult(
    r => Credential(r.nextInt, r.nextString, r.nextString, r.nextTimestamp)
  )

  def apply(name: String, password: String): Credential = Credential(0, name, password, null)

  implicit val format: OFormat[Credential] = (
    (__ \ "name").format[String](minLength[String](4) keepAnd maxLength[String](16)) ~
    (__ \ "password").format[String](minLength[String](4) keepAnd maxLength[String](16))
  )(
    (name, password) => Credential(name, password),
    unlift(user => Some((user.name, user.password)))
  )
}
