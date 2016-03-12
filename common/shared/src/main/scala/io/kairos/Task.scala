package io.kairos

import io.kairos.fault._
import io.kairos.id._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Task(
    id: TaskId,
    artifactId: ArtifactId,
    params: Map[String, AnyVal] = Map.empty,
    jobClass: String
)

object Task {

  sealed trait Outcome
  case object NotRunYet extends Outcome
  case class Success(result: Any) extends Outcome
  case class Failure(cause: Faults) extends Outcome
  case class Interrupted(reason: String) extends Outcome
  case class NeverRun(reason: String) extends Outcome
  case object NeverEnding extends Outcome

}