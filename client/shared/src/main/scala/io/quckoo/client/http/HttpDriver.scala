package io.quckoo.client.http

import upickle.default.{Reader => UReader, Writer => UWriter}

import io.quckoo.JobSpec
import io.quckoo.auth.{Credentials, InvalidCredentialsException, Passport}
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.client.core._
import io.quckoo.fault.Fault
import io.quckoo.id.JobId
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled, RegisterJob}

import slogging.LazyLogging

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 10/09/2016.
  */
final class HttpDriver(protected val transport: HttpTransport)
  extends Driver[Protocol.Http] with LazyLogging {
  type TransportRepr = HttpTransport

  private[this] abstract class JsonOp[Cmd[_] <: Command[_], In: UWriter, Rslt: UReader]
    extends Op[Cmd, In, Rslt] {

    override val unmarshall: Unmarshall[HttpResponse, Rslt] = {
      case HttpSuccess(payload) =>
        payload.as[Rslt]

      case err: HttpError =>
        HttpErrorException(err).left[Rslt]
    }

  }

  // -- Security

  trait HttpSecurityOps extends SecurityOps {
    implicit val authenticateOp: Op[AnonCmd, Credentials, Passport] =
      new Op[AnonCmd, Credentials, Passport] {

        override val marshall: Marshall[AnonCmd, Credentials, HttpRequest] = { cmd =>
          val creds = DataBuffer.fromString(s"${cmd.payload.username}:${cmd.payload.password}").toBase64
          val hdrs = Map(AuthorizationHeader -> s"Basic $creds")
          HttpRequest(HttpMethod.Post, LoginURI, cmd.timeout, headers = hdrs, None).right[Throwable]
        }

        override val unmarshall: Unmarshall[HttpResponse, Passport] = {
          case HttpSuccess(payload) =>
            Passport(payload.asString())

          case err: HttpError if err.statusCode == 401 =>
            InvalidCredentialsException.left[Passport]

          case err: HttpError if err.statusCode != 401 =>
            HttpErrorException(err).left[Passport]
        }
      }

    override implicit val signOutOp: Op[AuthCmd, Unit, Unit] =
      new Op[AuthCmd, Unit, Unit] {
        override val marshall: Marshall[AuthCmd, Unit, HttpRequest] = { cmd =>
          HttpRequest(HttpMethod.Post, LogoutURI, cmd.timeout, Map(cmd.passport.asHttpHeader), None).right[Throwable]
        }

        override val unmarshall: Unmarshall[HttpResponse, Unit] = {
          case HttpSuccess(_) => ().right[Throwable]
          case err: HttpError => HttpErrorException(err).left[Unit]
        }
      }
  }

  // -- Registry

  trait HttpRegistryOps extends RegistryOps {

    override implicit val registerJobOp: Op[AuthCmd, RegisterJob, ValidationNel[Fault, JobId]] =
      new JsonOp[AuthCmd, RegisterJob, ValidationNel[Fault, JobId]] {

        override val marshall: Marshall[AuthCmd, RegisterJob, HttpRequest] = { cmd =>
          DataBuffer(cmd.payload) map { entity =>
            val hdrs = JsonRequestHeaders + cmd.passport.asHttpHeader
            HttpRequest(HttpMethod.Put, JobsURI, cmd.timeout, hdrs, Some(entity))
          }
        }

      }

    override implicit val enableJobOp: Op[AuthCmd, JobId, JobEnabled] =
      new JsonOp[AuthCmd, JobId, JobEnabled] {

        override val marshall: Marshall[AuthCmd, JobId, HttpRequest] = { cmd =>
          val hrds = Map(cmd.passport.asHttpHeader)
          HttpRequest(HttpMethod.Post, s"$JobsURI/${cmd.payload}/enable", cmd.timeout, hrds, None).right[Throwable]
        }

      }

    override implicit val disableJobOp: Op[AuthCmd, JobId, JobDisabled] =
      new JsonOp[AuthCmd, JobId, JobDisabled] {

        override val marshall: Marshall[AuthCmd, JobId, HttpRequest] = { cmd =>
          val hrds = Map(cmd.passport.asHttpHeader)
          HttpRequest(HttpMethod.Post, s"$JobsURI/${cmd.payload}/disable", cmd.timeout, hrds, None).right[Throwable]
        }

      }

    override implicit val fetchJobOp: Op[AuthCmd, JobId, Option[JobSpec]] =
      new JsonOp[AuthCmd, JobId, Option[JobSpec]] {
        override val marshall: Marshall[AuthCmd, JobId, HttpRequest] = { cmd =>
          val hrds = Map(cmd.passport.asHttpHeader)
          HttpRequest(HttpMethod.Post, s"$JobsURI/${cmd.payload}", cmd.timeout, hrds, None).right[Throwable]
        }

        override val recover: Recover[Option[JobSpec]] = {
          case HttpErrorException(HttpError(code, _)) if code == 404 => none[JobSpec]
        }
      }
  }

  val ops = new Ops with HttpSecurityOps with HttpRegistryOps {

    override implicit val clusterStateOp: Op[AuthCmd, Unit, QuckooState] =
      new JsonOp[AuthCmd, Unit, QuckooState] {

        override val marshall: Marshall[AuthCmd, Unit, HttpRequest] = { cmd =>
          logger.debug("Retrieving current cluster state...")
          val hrds = Map(cmd.passport.asHttpHeader)
          HttpRequest(HttpMethod.Get, ClusterStateURI, cmd.timeout, headers = hrds, None).right[Throwable]
        }

    }

  }

}
