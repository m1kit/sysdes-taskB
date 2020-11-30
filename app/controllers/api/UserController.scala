package controllers.api

import actions.UserAction
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Results}
import models.{Credential, Users}
import utility.{AccessTokens, Responses}
import utility.Responses.{fail, success}

@Singleton
class UserController @Inject()(val users: Users)(val tokens: AccessTokens)(val userAction: UserAction)(
    cc: ControllerComponents
) extends AbstractController(cc) {

  def me(): Action[AnyContent] = userAction { request =>
    success("name" -> request.user.name)
  }

  def register: Action[JsValue] = Action(parse.json) { request =>
    request.body
      .validate[Credential]
      .fold(
        errors => fail(errors),
        user => {
          users.findByName(user.name) match {
            case Some(_) => fail(Results.BadRequest, "The user already exists.")
            case None =>
              if (users.register(user.name, user.password)) {
                success(Json.obj("name" -> user.name))
              } else {
                Responses.DBError
              }
          }
        }
      )
  }

  def withdraw: Action[AnyContent] = userAction { request =>
    if (users.delete(request.user.name)) {
      success("Bye...")
    } else {
      Responses.DBError
    }
  }

  def update: Action[JsValue] = userAction(parse.json) { request =>
    request.body
      .validate[Credential]
      .fold(
        errors => fail(errors),
        user =>
          if (user.name == request.user.name) {
            if (users.update(user.name, user.password)) {
              success()
            } else {
              Responses.DBError
            }
          } else {
            Responses.Forbidden
          }
      )
  }

  def login: Action[JsValue] = Action(parse.json) { request =>
    request.body
      .validate[Credential]
      .fold(
        errors => fail(errors),
        user => {
          users.authenticate(user.name, user.password) match {
            case Some(user) => success(Json.obj("token" -> tokens.encode(user)))
            case None       => fail(Results.BadRequest, "Wrong username or password")
          }
        }
      )
  }

}
