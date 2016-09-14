package io.quckoo.client.core

import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.id.JobId
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry.JobEnabled

import scala.concurrent.{ExecutionContext, Future}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 08/09/2016.
  */
trait Driver[P <: Protocol] {
  type TransportRepr <: Transport[P]

  protected val transport: TransportRepr

  trait Marshalling[Cmd[_] <: Command[_], In, Rslt] {
    val to: Marshall[Cmd, In, transport.Request]
    val from: Unmarshall[transport.Response, Rslt]
  }

  trait Ops {
    implicit val authenticateOp: Marshalling[AnonCmd, Credentials, Passport]
    implicit val clusterStateOp: Marshalling[AuthCmd, Unit, QuckooState]
    implicit val enableJobOp: Marshalling[AuthCmd, JobId, JobEnabled]
  }

  val ops: Ops

  final def invoke[Cmd[_] <: Command[_], In, Rslt](implicit
    ec: ExecutionContext,
    marshalling: Marshalling[Cmd, In, Rslt]
  ): Kleisli[Future, Cmd[In], Rslt] = {
    val encodeRequest  = Kleisli(marshalling.to).transform(try2Future)
    val decodeResponse = Kleisli(marshalling.from).transform(try2Future)

    encodeRequest >=> transport.send >=> decodeResponse
  }

}
