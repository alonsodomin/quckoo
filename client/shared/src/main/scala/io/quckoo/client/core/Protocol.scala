package io.quckoo.client.core

import io.quckoo.JobSpec
import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.fault.Fault
import io.quckoo.id.JobId
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled, JobNotFound, RegisterJob}

import scalaz._

/**
  * Created by alonsodomin on 17/09/2016.
  */
trait Protocol {
  type Request
  type Response

  trait Op {
    type Cmd[_] <: Command[_]
    type In
    type Rslt

    val marshall: Marshall[Cmd, In, Request]
    val unmarshall: Unmarshall[Response, Rslt]
    val recover: Recover[Rslt] = Recover.noRecover
  }

  trait AnonOp extends Op { type Cmd[X] = AnonCmd[X] }
  trait AuthOp extends Op { type Cmd[X] = AuthCmd[X] }

  trait SecurityOps {
    trait AuthenticateOp extends AnonOp { type In = Credentials; type Rslt = Passport }
    trait SingOutOp extends AuthOp { type In = Unit; type Rslt = Unit }

    implicit val authenticateOp: AuthenticateOp
    implicit val singOutOp: SingOutOp
  }

  trait RegistryOps {
    trait RegisterJobOp extends AuthOp { type In = RegisterJob; type Rslt = ValidationNel[Fault, JobId] }
    trait FetchJobOp extends AuthOp { type In = JobId; type Rslt = Option[JobSpec] }
    trait EnableJobOp extends AuthOp { type In = JobId; type Rslt = JobNotFound \/ JobEnabled }
    trait DisableJobOp extends AuthOp { type In = JobId; type Rslt = JobNotFound \/ JobDisabled }

    implicit val registerJobOp: RegisterJobOp
    implicit val fetchJobOp: FetchJobOp
    implicit val enableJobOp: EnableJobOp
    implicit val disableJobOp: DisableJobOp
  }

  trait ClusterOps {
    trait ClusterStateOp extends AuthOp { type In = Unit; type Rslt = QuckooState }

    implicit val clusterStateOp: ClusterStateOp
  }

  val ops: ClusterOps with RegistryOps with SecurityOps
}
