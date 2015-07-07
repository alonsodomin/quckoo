package io.chronos.scheduler.jobstore

import io.chronos.scheduler.{JobDefinition, Work}

/**
 * Created by domingueza on 07/07/15.
 */
trait WorkFactory {
  
  def createWork(jobDef: JobDefinition): Work
  
}
