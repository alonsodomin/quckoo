package io.chronos

import io.chronos.id.ExecutionId

/**
 * Created by aalonsodominguez on 12/07/15.
 */
package object topic {

  val AllResults = "/chronos/results"

  def execution(executionId: ExecutionId): String =
    s"/chronos/job/${executionId._1._1}/schedule/${executionId._1._2}/execution/${executionId._2}"

}
