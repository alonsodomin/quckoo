/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.api

import io.quckoo.{Fault, JobId, JobNotFound, JobSpec}
import io.quckoo.auth.Passport
import io.quckoo.protocol.registry._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{ValidationNel, \/}

/**
  * Created by alonsodomin on 13/12/2015.
  */
trait Registry {

  def enableJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[JobNotFound \/ JobEnabled]

  def disableJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[JobNotFound \/ JobDisabled]

  def fetchJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[JobSpec]]

  def registerJob(jobSpec: JobSpec)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[ValidationNel[Fault, JobId]]

  def fetchJobs(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Map[JobId, JobSpec]]

}
