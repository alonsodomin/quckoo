package io.chronos.scheduler

import io.chronos.JobSpec
import io.chronos.id._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by domingueza on 10/07/15.
  */
trait JobRegistry {

   def availableJobSpecs(implicit ec: ExecutionContext): Future[Seq[JobSpec]]

   def specById(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]]

   def registerJobSpec(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[JobId]

 }
