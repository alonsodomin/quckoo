package io.kairos.cluster

import io.kairos.Faults

/**
 * Created by aalonsodominguez on 17/08/15.
 */
package object scheduler {

  type TaskResult = Either[Faults, Any]

}
