package io.chronos.example

import io.chronos.Job

/**
 * Created by domingueza on 06/07/15.
 */
class PowerOfNJob extends Job {
  val n = 2

  override def execute(): String = {
    val n2 = n * n
    s"$n * $n = $n2"
  }

}
