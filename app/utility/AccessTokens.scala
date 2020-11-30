package utility

import javax.inject.{Inject, Singleton}
import models.User
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.libs.json.Json
import play.api.Configuration

@Singleton
class AccessTokens @Inject()(config: Configuration) {
  private val algo = JwtAlgorithm.HS256
  private val key  = config.get[String]("play.http.secret.key")

  def encode(user: User): String = JwtJson.encode(Json.toJsObject(user), key, algo)

  def decode(token: String): Option[User] =
    JwtJson.decodeJson(token, key, Seq(algo)).toOption.flatMap(json => json.validate[User].asOpt)

}
