package io.quckoo

import io.quckoo.fault._
import io.quckoo.id._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

final case class Task(
    id: TaskId,
    artifactId: ArtifactId,
    params: Map[String, AnyVal] = Map.empty,
    jobClass: String
)

object Task {

  sealed trait Outcome extends Serializable
  case object NotStarted extends Outcome
  case object Success extends Outcome
  case class Failure(cause: Fault) extends Outcome
  case class Interrupted(reason: String) extends Outcome
  case class NeverRun(reason: String) extends Outcome
  case object NeverEnding extends Outcome

}