package models

import java.sql.Timestamp
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, _}
import play.api.libs.json.Reads._

/**
  * Domain model of task
  * @param id          ID
  * @param title       タスク名
  * @param description タスクの説明
  * @param state       完了状態
  * @param cycle       繰り返しタスクの周期
  * @param createdAt   作成日時
  * @param deadline    締切日時
  * @param completedAt 完了日時
  */
case class Task(
    id: Int,
    title: String,
    description: String,
    state: TaskState,
    cycle: Option[Long],
    createdAt: Option[Timestamp],
    deadline: Option[Timestamp],
    completedAt: Option[Timestamp]
)

object Task extends DomainModel[Task] {
  import slick.jdbc.GetResult

  implicit def getResult: GetResult[Task] = GetResult(
    r =>
      Task(
        r.nextInt(),
        r.nextString(),
        r.nextString(),
        TaskState(r.nextInt()).get,
        r.nextLongOption(),
        r.nextTimestampOption(),
        r.nextTimestampOption(),
        r.nextTimestampOption()
      )
  )

  def apply(title: String, description: String): Task =
    Task(0, title, description, TaskState.Created, None, null, None, None)

  implicit val format: OFormat[Task] = (
    (__ \ "id").formatWithDefault[Int](0) ~
    (__ \ "title").format[String](minLength[String](1)) ~
    (__ \ "description").format[String](minLength[String](1)) ~
    (__ \ "state").format[TaskState] ~
    (__ \ "cycle").formatNullable[Long] ~
    (__ \ "created_at").formatNullable[String].inmap(_.map(Timestamp.valueOf), (_: Option[Timestamp]).map(_.toString)) ~
    (__ \ "deadline").formatNullable[String].inmap(_.map(Timestamp.valueOf), (_: Option[Timestamp]).map(_.toString)) ~
    (__ \ "completed_at").formatNullable[String].inmap(_.map(Timestamp.valueOf), (_: Option[Timestamp]).map(_.toString))
  )(
    (id, title, description, state, cycle, createdAt, deadline, completedAt) =>
      Task(id, title, description, state, cycle, createdAt, deadline, completedAt),
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
            task.completedAt
          )
        )
    )
  )
}
