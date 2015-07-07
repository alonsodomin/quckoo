package io.chronos.scheduler.example

import io.chronos.scheduler.{Job, JobDefinition}

import scala.concurrent.forkjoin.ThreadLocalRandom

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

object PowerOfNJobDef extends JobDefinition(jobId = "Power Of N", jobSpec = classOf[PowerOfNJob]) {

  def rnd = ThreadLocalRandom.current
  val n = rnd.nextInt(3, 20)
  
}