package io.chronos.scheduler.jobstore

import io.chronos.scheduler.JobDefinition
import io.chronos.scheduler.worker.Work

/**
 * Created by domingueza on 07/07/15.
 */
trait WorkFactory {
  
  def createWork(jobDef: JobDefinition): Work
  
}
