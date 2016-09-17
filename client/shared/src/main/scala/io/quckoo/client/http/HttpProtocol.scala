package io.quckoo.client.http

import upickle.default.{Reader => UReader, Writer => UWriter}

import io.quckoo.JobSpec
import io.quckoo.auth.{Credentials, InvalidCredentialsException, Passport}
import io.quckoo.client.core._
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.util.LawfulTry

import slogging.LazyLogging

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 17/09/2016.
  */
final class HttpProtocol extends Protocol with LazyLogging {
  type Request = HttpRequest
  type Response = HttpResponse

  private[this] abstract class JsonUnmarshall[O <: Op](implicit decoder: UReader[O#Rslt]) {
    val unmarshall: Unmarshall[HttpResponse, O#Rslt] = { res =>
      if (res.isFailure && res.entity.isEmpty) {
        HttpErrorException(res.statusLine).left[O#Rslt]
      }
      else res.entity.as[O#Rslt]
    }
  }

  // -- Security

  trait HttpSecurityOps extends SecurityOps {
    implicit val authenticateOp: AuthenticateOp = new AuthenticateOp {
      override val marshall: Marshall[AnonCmd, Credentials, HttpRequest] = { cmd =>
        val creds = DataBuffer.fromString(s"${cmd.payload.username}:${cmd.payload.password}").toBase64
        val hdrs = Map(AuthorizationHeader -> s"Basic $creds")
        HttpRequest(HttpMethod.Post, LoginURI, cmd.timeout, headers = hdrs).right[Throwable]
      }

      override val unmarshall: Unmarshall[HttpResponse, Passport] = { res =>
        if (res.isSuccess) Passport(res.entity.asString())
        else {
          if (res.statusCode == 401) InvalidCredentialsException.left[Passport]
          else HttpErrorException(res.statusLine).left[Passport]
        }
      }
    }

    override implicit val singOutOp: SingOutOp = new SingOutOp {
      override val marshall: Marshall[AuthCmd, Unit, HttpRequest] = { cmd =>
        HttpRequest(HttpMethod.Post, LogoutURI, cmd.timeout, Map(cmd.passport.asHttpHeader)).right[Throwable]
      }
      override val unmarshall: Unmarshall[HttpResponse, Unit] = { res =>
        if (res.isSuccess) ().right[Throwable]
        else HttpErrorException(res.statusLine).left[Unit]
      }
    }
  }

  // -- Registry

  trait HttpRegistryOps extends RegistryOps {
    override implicit val registerJobOp: RegisterJobOp = new JsonUnmarshall[RegisterJobOp] with RegisterJobOp {
      override val marshall: Marshall[AuthCmd, RegisterJob, HttpRequest] = { cmd =>
        DataBuffer(cmd.payload) map { entity =>
          val hdrs = JsonRequestHeaders + cmd.passport.asHttpHeader
          HttpRequest(HttpMethod.Put, JobsURI, cmd.timeout, hdrs, entity)
        }
      }
    }

    override implicit val fetchJobOp: FetchJobOp = new JsonUnmarshall[FetchJobOp] with FetchJobOp {

      override val marshall: Marshall[AuthCmd, JobId, HttpRequest] = { cmd =>
        val hdrs = Map(cmd.passport.asHttpHeader)
        HttpRequest(HttpMethod.Get, s"$JobsURI/${cmd.payload}", cmd.timeout, hdrs).right[Throwable]
      }

      override val unmarshall: Unmarshall[HttpResponse, Option[JobSpec]] = { res =>
        if (res.statusCode == 404) none[JobSpec].right[Throwable]
        else super.unmarshall(res).asInstanceOf[LawfulTry[Option[JobSpec]]]
      }

    }

    override implicit val fetchJobsOp: FetchJobsOp = new JsonUnmarshall[FetchJobsOp] with FetchJobsOp {
      override val marshall: Marshall[AuthCmd, Unit, HttpRequest] = { cmd =>
        val hdrs = Map(cmd.passport.asHttpHeader)
        HttpRequest(HttpMethod.Get, JobsURI, cmd.timeout, hdrs).right[Throwable]
      }
    } 

    override implicit val enableJobOp: EnableJobOp = new JsonUnmarshall[EnableJobOp] with EnableJobOp {
      override val marshall: Marshall[AuthCmd, JobId, HttpRequest] = { cmd =>
        val hdrs = Map(cmd.passport.asHttpHeader)
        HttpRequest(HttpMethod.Post, s"$JobsURI/${cmd.payload}/enable", cmd.timeout, hdrs).right[Throwable]
      }
    }

    override implicit val disableJobOp: DisableJobOp = new JsonUnmarshall[DisableJobOp] with DisableJobOp {
      override val marshall: Marshall[AuthCmd, JobId, HttpRequest] = { cmd =>
        val hdrs = Map(cmd.passport.asHttpHeader)
        HttpRequest(HttpMethod.Post, s"$JobsURI/${cmd.payload}/disable", cmd.timeout, hdrs).right[Throwable]
      }
    }
  }

  // -- Cluster

  trait HttpClusterOps extends ClusterOps {
    override implicit val clusterStateOp: ClusterStateOp = new JsonUnmarshall[ClusterStateOp] with ClusterStateOp {
      override val marshall: Marshall[AuthCmd, Unit, HttpRequest] = { cmd =>
        logger.debug("Retrieving current cluster state...")
        val hdrs = Map(cmd.passport.asHttpHeader)
        HttpRequest(HttpMethod.Get, ClusterStateURI, cmd.timeout, headers = hdrs).right[Throwable]
      }
    }
  }

  val ops = new HttpClusterOps with HttpRegistryOps with HttpSecurityOps {}
}
