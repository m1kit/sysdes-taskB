package controllers.api

import actions.UserAction
import javax.inject.Inject
import models.{Task, TaskDetail, TaskState, Tasks, Users}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Results}
import utility.{AccessTokens, Responses}
import utility.Responses.{fail, success}

@Inject
class TaskController @Inject()(val tasks: Tasks)(val users: Users)(val tokens: AccessTokens)(val userAction: UserAction)(
    cc: ControllerComponents
) extends AbstractController(cc) {

  def list(): Action[AnyContent] = userAction { request =>
    val state = request
      .getQueryString("state")
      .map(s => s.split(",").toSeq)
      .getOrElse(Seq[String]())
      .map(TaskState(_))
    val page = request
      .getQueryString("page")
      .flatMap(_.toIntOption)
      .getOrElse(0)
    if (state.exists(_.isEmpty)) {
      fail(Results.BadRequest, "Invalid state")
    } else {
      val filter = if (state.isEmpty) TaskState.values else state.map(_.get)
      val tag    = request.getQueryString("tag")
      val result = tasks.list(request.user.id, filter, tag, page)
      success(Json.toJson(result))
    }
  }

  def detail(id: Int): Action[AnyContent] = userAction { request =>
    {
      if (tasks.checkPermission(id, request.user.id)) {
        tasks.getDetails(id) match {
          case None       => Responses.NotFound
          case Some(task) => success(Json.toJson(task))
        }
      } else Responses.Forbidden
    }
  }

  def create(): Action[JsValue] = userAction(parse.json) { request =>
    //println(request.body)
    request.body
      .validate[Task]
      .fold(
        errors => fail(errors),
        task =>
          tasks
            .createTask(task)
            .flatMap(task => {
              if (tasks.addUser(task.id, request.user.id)) Some(task)
              else None
            }) match {
            case Some(task) => success(Json.toJson(task))
            case None       => Responses.DBError
          }
      )
    //success(Json.toJson(tasks.list(request.user.id)))
  }

  def addUser(task: Int): Action[JsValue] = userAction(parse.json) { request =>
    if (tasks.checkPermission(task, request.user.id)) {
      request.body
        .validate[String]
        .fold(
          error => Responses.fail(error),
          name =>
            users.findByName(name) match {
              case None => Responses.fail(Results.NotFound, "User not found")
              case Some(user) =>
                if (tasks.addUser(task, user.id)) {
                  tasks.getDetails(task) match {
                    case Some(details) => success(Json.toJson(details))
                    case None          => Responses.DBError
                  }
                } else Responses.fail(Results.BadRequest, "already added")
            }
        )
    } else Responses.Forbidden
  }

  def removeUser(task: Int, user: String): Action[AnyContent] = userAction { request =>
    if (tasks.checkPermission(task, request.user.id)) {
      users.findByName(user) match {
        case None => Responses.fail(Results.NotFound, "User not found")
        case Some(user) =>
          if (tasks.addUser(task, user.id)) {
            tasks.getDetails(task) match {
              case Some(details) => success(Json.toJson(details))
              case None          => Responses.DBError
            }
          } else Responses.fail(Results.BadRequest, "already added")
      }
    } else {
      Responses.Forbidden
    }
  }

  def addTag(task: Int): Action[JsValue] = userAction(parse.json) { request =>
    if (tasks.checkPermission(task, request.user.id)) {
      request.body
        .validate[String]
        .filter(_.length >= 1)
        .fold(
          error => Responses.fail(error),
          tag =>
            if (tasks.addTag(task, tag)) {
              tasks.getDetails(task) match {
                case Some(details) => success(Json.toJson(details))
                case None          => Responses.DBError
              }
            } else Responses.DBError
        )
    } else {
      Responses.Forbidden
    }
  }

  def removeTag(task: Int, tag: String): Action[AnyContent] = userAction { request =>
    if (tasks.checkPermission(task, request.user.id)) {
      if (tasks.removeTag(task, tag)) {
        tasks.getDetails(task) match {
          case Some(details) => success(Json.toJson(details))
          case None          => Responses.DBError
        }
      } else Responses.DBError
    } else {
      Responses.Forbidden
    }
  }
}
