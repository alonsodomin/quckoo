package io.quckoo.cluster

import akka.actor.Props
import io.quckoo.JobSpec
import io.quckoo.fault.Faults
import io.quckoo.id._

/**
 * Created by aalonsodominguez on 17/08/15.
 */
package object scheduler {

  type TaskResult = Either[Faults, Any]

}
