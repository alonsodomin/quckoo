package io.kairos.cluster.scheduler

import akka.actor.Props
import io.kairos.JobSpec
import io.kairos.id._

/**
 * Created by aalonsodominguez on 20/08/15.
 */
package object execution {

  type ExecutionProps = (TaskId, JobSpec) => Props

}
