package models

import java.sql.Timestamp

import play.api.libs.functional.syntax.unlift
import play.api.libs.json.Reads.minLength
import play.api.libs.json.{__, OFormat}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._

case class TaskDetail(
    id: Int,
    title: String,
    description: String,
    state: TaskState,
    cycle: Option[Long],
    createdAt: Option[Timestamp],
    deadline: Option[Timestamp],
    completedAt: Option[Timestamp],
    users: Seq[User],
    tags: Seq[String]
)

object TaskDetail {
  def apply(task: Task, users: Seq[User], tags: Seq[String]): TaskDetail = {
    TaskDetail(
      task.id,
      task.title,
      task.description,
      task.state,
      task.cycle,
      task.createdAt,
      task.deadline,
      task.completedAt,
      users,
      tags
    )
  }

  implicit val format: OFormat[TaskDetail] = (
    (__ \ "id").formatWithDefault[Int](0) ~
    (__ \ "title").format[String](minLength[String](1)) ~
    (__ \ "description").format[String](minLength[String](1)) ~
    (__ \ "state").format[TaskState] ~
    (__ \ "cycle").formatNullable[Long] ~
    (__ \ "created_at").formatNullable[String].inmap(_.map(Timestamp.valueOf), (_: Option[Timestamp]).map(_.toString)) ~
    (__ \ "deadline").formatNullable[String].inmap(_.map(Timestamp.valueOf), (_: Option[Timestamp]).map(_.toString)) ~
    (__ \ "completed_at")
      .formatNullable[String]
      .inmap(_.map(Timestamp.valueOf), (_: Option[Timestamp]).map(_.toString)) ~
    (__ \ "users").format[Seq[User]] ~
    (__ \ "tags").format[Seq[String]]
  )(
    (id, title, description, state, cycle, createdAt, deadline, completedAt, users, tags) =>
      TaskDetail(id, title, description, state, cycle, createdAt, deadline, completedAt, users, tags),
    unlift(
      task =>
        Some(
          (
            task.id,
            task.title,
            task.description,
            task.state,
            task.cycle,
            task.createdAt,
            task.deadline,
            task.completedAt,
            task.users,
            task.tags
          )
        )
    )
  )

}
