package model

import io.chronos.Execution
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by aalonsodominguez on 14/07/15.
 */
object ExecutionModel {

  implicit def format: Format[ExecutionModel] = (
      (__ \ "executionId").format[String] ~
      (__ \ "status").format[String]
  )(ExecutionModel.apply, unlift(ExecutionModel.unapply))

}

case class ExecutionModel(executionId: String, status: String) {

  def this(execution: Execution) {
    this(execution.executionId, execution.stage.toString)
  }

}
