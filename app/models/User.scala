package models

import java.sql.Timestamp
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}
import play.api.libs.json.Reads._

/**
  * Domain model of a user
  * @param id         ID
  * @param name       名前
  * @param createdAt  作成日時
  */
case class User(id: Int, name: String, createdAt: Option[Timestamp])

object User extends DomainModel[User] {
  import slick.jdbc.GetResult
  implicit def getResult: GetResult[User] = GetResult(
    r => User(r.nextInt, r.nextString, r.nextTimestampOption())
  )

  def apply(id: Int, name: String): User = User(id, name, null)

  implicit val format: OFormat[User] = (
    (__ \ "id").format[Int](min[Int](0)) ~
    (__ \ "name").format[String](minLength[String](4) keepAnd maxLength[String](16)) ~
    (__ \ "created_at").formatNullable[String].inmap(_.map(Timestamp.valueOf), (_: Option[Timestamp]).map(_.toString))
  )(
    (id, name, createdAt) => User(id, name, createdAt),
    unlift(user => Some((user.id, user.name, user.createdAt)))
  )
}
