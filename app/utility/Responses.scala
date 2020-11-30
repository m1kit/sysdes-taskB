package utility

import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsPath, Json, JsonValidationError}
import play.api.mvc.{Result, Results}

object Responses {
  def success(data: JsValueWrapper = Json.obj()): Result =
    Results.Ok(
      Json.obj(
        "status"      -> "OK",
        "data"        -> data,
        "description" -> Json.arr("Success")
      )
    )

  def fail(status: Results.Status, description: String*): Result =
    status(
      Json.obj(
        "status"      -> "Fail",
        "data"        -> Json.obj(),
        "description" -> description
      )
    )

  def fail(validationError: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): Result = {
    val msgs = validationError.flatMap(e => e._2.flatMap(k => k.messages.map(m => f"Validation Error: ${m}@${e._1}")))
    fail(Results.BadRequest, msgs.toSeq: _*)
  }

  val NotFound: Result     = fail(Results.NotFound, "The requested resource does not exist")
  val Unauthorized: Result = fail(Results.Unauthorized, "Authorization Required")
  val Forbidden: Result    = fail(Results.Forbidden, "You don't have the permission to access the requested resource")
  val DBError: Result      = fail(Results.InternalServerError, "Failed to access the DB")
}
