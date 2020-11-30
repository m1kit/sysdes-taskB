package models

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.db.slick.{DatabaseConfigProvider => DBConfigProvider}

/**
  * task テーブルへの Accessor
  */
@Singleton
class Tasks @Inject()(dbcp: DBConfigProvider)(implicit ec: ExecutionContext) extends Dao(dbcp) {

  import profile.api._
  import utility.Await

  def list(
      user: Int,
      states: Seq[TaskState] = TaskState.values,
      tag: Option[String],
      page: Int,
      pageSize: Int = 5
  ): Seq[Task] = {
    var stateFilter = "FALSE"
    states.foreach(state => stateFilter += f" OR task.state = ${state.code}")
    println(stateFilter)
    println(tag)
    tag match {
      case Some(tag) =>
        Await.result(
          db.run(
            sql"""
        SELECT task.id, task.title, task.description, task.state, task.cycle, task.created_at, task.deadline, task.completed_at
        FROM task
        INNER JOIN task_user, task_tag
        ON task.id = task_user.task_id AND task.id = task_tag.task_id
        WHERE task_user.user_id = $user AND (#$stateFilter) AND task_tag.tag = $tag
        ORDER BY task.state, task.deadline ASC
        LIMIT #$page
        OFFSET #${page * pageSize}
        """.as[Task]
          )
        )
      case None =>
        Await.result(
          db.run(
            sql"""
        SELECT task.id, task.title, task.description, task.state, task.cycle, task.created_at, task.deadline, task.completed_at
        FROM task
        INNER JOIN task_user
        ON task.id = task_user.task_id
        WHERE task_user.user_id = $user AND (#$stateFilter)
        ORDER BY task.state, task.deadline
        LIMIT #$pageSize
        OFFSET #${page * pageSize}
        """.as[Task]
          )
        )
    }
  }

  def createTask(task: Task) =
    Await.result({
      db.run(sql"""
        INSERT INTO task(title, description, state, cycle, deadline, completed_at)
        VALUES (${task.title}, ${task.description}, ${task.state.code}, ${task.cycle}, ${task.deadline}, ${task.completedAt})
         """.asUpdate) flatMap (_ => db.run(sql"""
           SELECT id, title, description, state, cycle, created_at, deadline, completed_at 
           FROM task 
           WHERE id=LAST_INSERT_ID()
           """.as[Task].headOption))
    })

  def checkPermission(task: Int, user: Int): Boolean =
    Await.result(
      db.run(
        sql"""
        SELECT COUNT(*)
        FROM task_user
        WHERE task_id = $task AND user_id = $user
        """.as[Int].head
      )
    ) != 0

  def addUser(task: Int, user: Int): Boolean = {
    Await.result(
      db.run(sql"INSERT IGNORE INTO task_user(task_id, user_id) VALUES ($task, $user)".asUpdate)
    ) != 0
  }

  def removeUser(task: Int, user: Int): Boolean =
    Await.result(
      db.run(sql"DELETE FROM task_user WHERE task_id=$task AND user_id=$user".asUpdate)
    ) != 0

  def addTag(task: Int, tag: String): Boolean = {
    Await.result(
      db.run(sql"INSERT IGNORE INTO task_tag(task_id, tag) VALUES ($task, $tag)".asUpdate)
    ) != 0
  }

  def removeTag(task: Int, tag: String): Boolean =
    Await.result(
      db.run(sql"DELETE FROM task_tag WHERE task_id=$task AND tag=$tag".asUpdate)
    ) != 0

  def get(task: Int): Option[Task] = Await.result(
    db.run(
      sql"""
        SELECT task.id, task.title, task.description, task.state, task.cycle, task.created_at, task.deadline, task.completed_at
        FROM task
        WHERE task.id = $task
        """.as[Task].headOption
    )
  )

  def getDetails(id: Int): Option[TaskDetail] =
    get(id).map(task => {
      val users = getUsers(id)
      val tags  = getTags(id)
      TaskDetail(task, users, tags)
    })

  def getUsers(task: Int): Seq[User] = Await.result(
    db.run(sql"""
        SELECT user.id, user.name, user.created_at
        FROM user
        INNER JOIN task_user
        ON task_user.user_id = user.id AND task_user.task_id = $task
         """.as[User])
  )

  def getTags(task: Int): Seq[String] = Await.result(
    db.run(sql"""
        SELECT task_tag.tag
        FROM task_tag
        WHERE task_tag.task_id = $task
         """.as[String])
  )
}
