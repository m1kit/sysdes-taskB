package actions

import com.google.inject.Inject
import play.api.mvc.{ActionBuilder, ActionRefiner, AnyContent, BodyParsers, Request, Result}
import utility.{AccessTokens, Responses}

import scala.concurrent.{ExecutionContext, Future}

class UserAction @Inject()(val parser: BodyParsers.Default)(val tokens: AccessTokens)(
    implicit val executionContext: ExecutionContext
) extends ActionBuilder[UserRequest, AnyContent]
    with ActionRefiner[Request, UserRequest] {
  override def refine[A](request: Request[A]): Future[Either[Result, UserRequest[A]]] = {
    request.headers
      .get("Authorization")
      .flatMap("^Bearer (.+)$".r.findFirstMatchIn)
      .map(ma => ma.group(1))
      .flatMap(tokens.decode) match {
      case None       => Future.successful(Left(Responses.Unauthorized))
      case Some(user) => Future.successful(Right(UserRequest(user, request)))
    }
  }
}
