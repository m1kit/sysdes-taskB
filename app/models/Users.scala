package models

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext
import play.api.db.slick.{DatabaseConfigProvider => DBConfigProvider}
import utility.Digest

/**
  * user テーブルへの Accessor
  */
@Singleton
class Users @Inject()(dbcp: DBConfigProvider)(implicit ec: ExecutionContext) extends Dao(dbcp) {

  import profile.api._
  import utility.Await

  val table = "user"

  def findByName(name: String): Option[User] = Await.result(
    db.run(sql"SELECT id, name, created_at FROM #$table WHERE name=$name".as[User].headOption)
  )

  def authenticate(name: String, password: String): Option[User] = {
    val digest = Digest(password)
    Await.result(
      db.run(
        sql"SELECT id, name, created_at FROM #$table WHERE name=$name AND password=$digest"
          .as[User]
          .headOption
      )
    )
  }

  def register(name: String, password: String): Boolean = {
    val digest = Digest(password)
    Await.result(
      db.run(sql"INSERT INTO #$table(name, password) VALUES ($name, $digest)".asUpdate)
    ) != 0
  }

  def delete(name: String): Boolean = {
    Await.result(
      db.run(sql"DELETE FROM #$table WHERE name=$name".asUpdate)
    ) != 0
  }

  def update(name: String, password: String): Boolean = {
    val digest = Digest(password)
    Await.result(
      db.run(sql"UPDATE #$table SET password=$digest WHERE name=$name".asUpdate)
    ) != 0
  }

}
