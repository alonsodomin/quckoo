package io.kairos.cluster

import akka.actor.Props
import io.kairos.JobSpec
import io.kairos.fault.Faults
import io.kairos.id._

/**
 * Created by aalonsodominguez on 17/08/15.
 */
package object scheduler {

  type TaskResult = Either[Faults, Any]
  type ExecutionProps = (TaskId, JobSpec) => Props

}
