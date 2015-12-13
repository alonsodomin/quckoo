package io.kairos.scheduler.execution

import io.kairos.cluster._
import io.kairos.id.PlanId

/**
 * Created by aalonsodominguez on 26/08/15.
 */
object Execution {

  def apply(planId: PlanId, task: Task): Execution = new Execution(planId, task, NotRunYet)

  sealed trait Outcome
  case object NotRunYet extends Outcome
  case class Success(result: Any) extends Outcome
  case class Failure(cause: TaskFailureCause) extends Outcome
  case class Interrupted(reason: String) extends Outcome
  case class NeverRun(reason: String) extends Outcome
  case object NeverEnding extends Outcome

}

class Execution private (val planId: PlanId, val task: Task, val outcome: Execution.Outcome) extends Serializable {

  import Execution._
  
  private[execution] def <<= (outcome: Outcome): Execution = new Execution(planId, task, outcome)
  
}
