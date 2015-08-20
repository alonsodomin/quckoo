package io.chronos.scheduler

import akka.actor.Props
import io.chronos.JobSpec
import io.chronos.id._

/**
 * Created by aalonsodominguez on 20/08/15.
 */
package object execution {

  type ExecutionProps = (PlanId, JobSpec) => Props

}
