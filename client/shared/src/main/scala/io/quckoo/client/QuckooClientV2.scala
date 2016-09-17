package io.quckoo.client

import io.quckoo.JobSpec
import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.client.core._
import io.quckoo.fault.Fault
import io.quckoo.id.JobId
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled, JobNotFound, RegisterJob}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

import scalaz._

/**
  * Created by alonsodomin on 10/09/2016.
  */
abstract class QuckooClientV2[P <: Protocol](driver: Driver[P]) {
  import driver.ops._

  def authenticate(username: String, password: String)(
    implicit
    ec: ExecutionContext, timeout: Duration
  ): Future[Passport] = {
    val cmd = AnonCmd(Credentials(username, password), timeout)
    driver.invoke[AuthenticateOp].run(cmd)
  }

  def signOut(
    implicit
    ec: ExecutionContext, timeout: Duration, passport: Passport
  ): Future[Unit] =
    driver.invoke[SingOutOp].run(AuthCmd((), timeout, passport))

  def clusterState(
    implicit
    ec: ExecutionContext, timeout: Duration, passport: Passport
  ): Future[QuckooState] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[ClusterStateOp].run(cmd)
  }

  def registerJob(job: JobSpec)(
    implicit
    ec: ExecutionContext, timeout: Duration, passport: Passport
  ): Future[ValidationNel[Fault, JobId]] = {
    val cmd = AuthCmd(RegisterJob(job), timeout, passport)
    driver.invoke[RegisterJobOp].run(cmd)
  }

  def fetchJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: Duration, passport: Passport
  ): Future[Option[JobSpec]] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[FetchJobOp].run(cmd)
  }

  def enableJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: Duration, passport: Passport
  ): Future[JobNotFound \/ JobEnabled] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[EnableJobOp].run(cmd)
  }

  def disableJob(jobId: JobId)(
    implicit
    ec: ExecutionContext, timeout: Duration, passport: Passport
  ): Future[JobNotFound \/ JobDisabled] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[DisableJobOp].run(cmd)
  }

}
