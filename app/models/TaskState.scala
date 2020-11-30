package models
import scala.util.Try
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}
import play.api.libs.json.Reads._

sealed abstract class TaskState(val code: Int, val name: String)
object TaskState {
  case object Created   extends TaskState(0, "created")
  case object Ongoing   extends TaskState(1, "ongoing")
  case object Completed extends TaskState(2, "completed")
  case object Deleted   extends TaskState(3, "deleted")

  def apply(code: Int): Option[TaskState] = Try(values(code)).toOption
  def apply(name: String): Option[TaskState] = {
    name match {
      case Created.name   => Some(Created)
      case Ongoing.name   => Some(Ongoing)
      case Completed.name => Some(Completed)
      case Deleted.name   => Some(Deleted)
      case _              => None
    }
  }

  def unapply(state: TaskState): Option[String] = Some(state.name)

  val values = Seq(Created, Ongoing, Completed, Deleted)
  implicit val format = new Format[TaskState] {
    override def reads(j: JsValue): JsResult[TaskState] =
      j.validate[String].asOpt.flatMap(TaskState(_)) match {
        case Some(state) => JsSuccess(state)
        case None        => JsError("Invalid state")
      }
    override def writes(se: TaskState): JsValue =
      JsString(unlift(TaskState.unapply)(se))
  }
}
