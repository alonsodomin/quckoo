package io.kairos.cluster.scheduler.execution

import io.kairos.cluster._
import io.kairos.fault.Faults
import io.kairos.id.PlanId

/**
 * Created by aalonsodominguez on 26/08/15.
 */
object ExecutionState {

  def apply(planId: PlanId, task: Task): ExecutionState = new ExecutionState(planId, task, NotRunYet)

  sealed trait Outcome
  case object NotRunYet extends Outcome
  case class Success(result: Any) extends Outcome
  case class Failure(cause: Faults) extends Outcome
  case class Interrupted(reason: String) extends Outcome
  case class NeverRun(reason: String) extends Outcome
  case object NeverEnding extends Outcome

}

class ExecutionState private(val planId: PlanId, val task: Task, val outcome: ExecutionState.Outcome) extends Serializable {

  import ExecutionState._

  private[execution] def <<= (outcome: Outcome): ExecutionState = new ExecutionState(planId, task, outcome)
  
}
