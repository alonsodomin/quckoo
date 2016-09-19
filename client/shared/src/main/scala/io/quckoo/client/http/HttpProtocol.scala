package io.quckoo.client.http

import upickle.default.{Reader => UReader, Writer => UWriter}

import io.quckoo.{ExecutionPlan, JobSpec, TaskExecution}
import io.quckoo.auth.{Credentials, InvalidCredentialsException, Passport}
import io.quckoo.client.core._
import io.quckoo.id.{JobId, PlanId, TaskId}
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler.ScheduleJob
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._
import io.quckoo.util.LawfulTry

import slogging.LazyLogging

import scalaz._

/**
  * Created by alonsodomin on 17/09/2016.
  */
sealed trait HttpProtocol extends Protocol with LazyLogging {
  type Request = HttpRequest
  type Response = HttpResponse

  def jsonMarshall[O <: Op](
    method: HttpMethod, uriFor: O#Cmd[O#In] => String, writeBody: Boolean = true)(
    implicit encoder: UWriter[O#In]
  ): Marshall[O#Cmd, O#In, HttpRequest] = Marshall[O#Cmd, O#In, HttpRequest] { cmd =>
    def createReq(passport: Option[Passport], entity: Option[LawfulTry[DataBuffer]]) = {
      val data = entity.getOrElse(LawfulTry.success(DataBuffer.Empty))
      val baseHeaders = passport.map(pass => Map(authHeader(pass))).getOrElse(Map.empty[String, String])
      data.map { buff =>
        val hdrs = {
          if (!buff.isEmpty) baseHeaders ++ JsonRequestHeaders
          else baseHeaders
        }
        val baseRequest = HttpRequest(method, uriFor(cmd), cmd.timeout, hdrs)
        if (!buff.isEmpty) baseRequest.copy(entity = buff)
        else baseRequest
      }
    }

    cmd match {
      case AuthCmd(_, _, passport) if !writeBody => createReq(Some(passport), None)
      case AuthCmd(payload, _, passport)   => createReq(Some(passport), Some(DataBuffer(payload.asInstanceOf[O#In])))

      case AnonCmd(_, _) if !writeBody => createReq(None, None)
      case AnonCmd(payload, _)   => createReq(None, Some(DataBuffer(payload.asInstanceOf[O#In])))
    }
  }

  def jsonUnmarshall[O <: Op](implicit decoder: UReader[O#Rslt]): Unmarshall[HttpResponse, O#Rslt] = Unmarshall { res =>
    if (res.isFailure && res.entity.isEmpty) {
      -\/(HttpErrorException(res.statusLine))
    }
    else res.entity.as[O#Rslt](decoder)
  }

  private[this] abstract class JsonUnmarshall[O <: Op](implicit decoder: UReader[O#Rslt]) {
    val unmarshall: Unmarshall[HttpResponse, O#Rslt] = Unmarshall { res =>
      if (res.isFailure && res.entity.isEmpty) {
        -\/(HttpErrorException(res.statusLine))
      }
      else res.entity.as[O#Rslt]
    }
  }

  @inline private[this] def authHeader(passport: Passport): (String, String) =
    AuthorizationHeader -> s"Bearer ${passport.token}"

  // -- Security

  trait HttpSecurityOps extends SecurityOps {

    implicit val authenticateOp: AuthenticateOp = new AuthenticateOp {
      override val marshall = Marshall[AnonCmd, Credentials, HttpRequest] { cmd =>
        val creds = DataBuffer.fromString(s"${cmd.payload.username}:${cmd.payload.password}").toBase64
        val hdrs = Map(AuthorizationHeader -> s"Basic $creds")

        LawfulTry.success(HttpRequest(HttpMethod.Post, LoginURI, cmd.timeout, headers = hdrs))
      }

      override val unmarshall: Unmarshall[HttpResponse, Passport] = Unmarshall { res =>
        if (res.isSuccess) Passport(res.entity.asString())
        else LawfulTry.failed {
          if (res.statusCode == 401) InvalidCredentialsException
          else HttpErrorException(res.statusLine)
        }
      }
    }

    override implicit val singOutOp: SingOutOp = new SingOutOp {
      override val marshall = Marshall[AuthCmd, Unit, HttpRequest] { cmd =>
        LawfulTry.success {
          HttpRequest(HttpMethod.Post, LogoutURI, cmd.timeout, Map(authHeader(cmd.passport)))
        }
      }

      override val unmarshall: Unmarshall[HttpResponse, Unit] = Unmarshall { res =>
        if (res.isSuccess) LawfulTry.unit
        else LawfulTry.failed(HttpErrorException(res.statusLine))
      }
    }
  }

  // -- Cluster

  trait HttpClusterOps extends ClusterOps {
    override implicit val clusterStateOp: ClusterStateOp = new JsonUnmarshall[ClusterStateOp] with ClusterStateOp {
      override val marshall: Marshall[AuthCmd, Unit, HttpRequest] =
        jsonMarshall[ClusterStateOp](HttpMethod.Get, _ => ClusterStateURI)

    }
  }

  // -- Registry

  trait HttpRegistryOps extends RegistryOps {
    import scalaz.std.option._

    override implicit val registerJobOp: RegisterJobOp = new JsonUnmarshall[RegisterJobOp] with RegisterJobOp {
      override val marshall: Marshall[AuthCmd, RegisterJob, HttpRequest] =
        jsonMarshall[RegisterJobOp](HttpMethod.Put, _ => JobsURI)
    }

    override implicit val fetchJobOp: FetchJobOp = new JsonUnmarshall[FetchJobOp] with FetchJobOp {
      override val marshall: Marshall[AuthCmd, JobId, HttpRequest] =
        jsonMarshall[FetchJobOp](HttpMethod.Get, cmd => s"$JobsURI/${cmd.payload}", writeBody = false)

      override val unmarshall: Unmarshall[HttpResponse, Option[JobSpec]] = Unmarshall { res =>
        if (res.statusCode == 404) LawfulTry.success(none[JobSpec])
        else super.unmarshall(res).asInstanceOf[LawfulTry[Option[JobSpec]]]
      }

    }

    override implicit val fetchJobsOp: FetchJobsOp = new JsonUnmarshall[FetchJobsOp] with FetchJobsOp {
      override val marshall: Marshall[AuthCmd, Unit, HttpRequest] =
        jsonMarshall[FetchJobsOp](HttpMethod.Get, _ => JobsURI)
    }

    override implicit val enableJobOp: EnableJobOp = new JsonUnmarshall[EnableJobOp] with EnableJobOp {
      override val marshall: Marshall[AuthCmd, JobId, HttpRequest] =
        jsonMarshall[EnableJobOp](HttpMethod.Post, cmd => s"$JobsURI/${cmd.payload}/enable", writeBody = false)
    }

    override implicit val disableJobOp: DisableJobOp = new JsonUnmarshall[DisableJobOp] with DisableJobOp {
      override val marshall: Marshall[AuthCmd, JobId, HttpRequest] =
        jsonMarshall[DisableJobOp](HttpMethod.Post, cmd => s"$JobsURI/${cmd.payload}/disable", writeBody = false)
    }
  }

  // -- Scheduler

  trait HttpSchedulerOps extends SchedulerOps {
    override implicit val executionPlansOp: ExecutionPlansOp = new JsonUnmarshall[ExecutionPlansOp] with ExecutionPlansOp {
      override val marshall: Marshall[AuthCmd, Unit, HttpRequest] =
        jsonMarshall[ExecutionPlansOp](HttpMethod.Get, _ => ExecutionPlansURI)
    }

    override implicit val executionPlanOp: ExecutionPlanOp =
      new JsonUnmarshall[ExecutionPlanOp] with ExecutionPlanOp {
        import scalaz.std.option._

        override val marshall: Marshall[AuthCmd, PlanId, HttpRequest] =
          jsonMarshall[ExecutionPlanOp](HttpMethod.Get, cmd => s"$ExecutionPlansURI/${cmd.payload}", writeBody = false)

        override val unmarshall: Unmarshall[HttpResponse, Option[ExecutionPlan]] = Unmarshall { res =>
          if (res.statusCode == 404) LawfulTry.success(none[ExecutionPlan])
          else super.unmarshall(res).asInstanceOf[LawfulTry[Option[ExecutionPlan]]]
        }
      }

    override implicit val executionsOp: ExecutionsOp = new JsonUnmarshall[ExecutionsOp] with ExecutionsOp {
      override val marshall: Marshall[AuthCmd, Unit, HttpRequest] =
        jsonMarshall[ExecutionsOp](HttpMethod.Get, _ => TaskExecutionsURI)
    }

    override implicit val executionOp: ExecutionOp = new JsonUnmarshall[ExecutionOp] with ExecutionOp {
      import scalaz.std.option._

      override val marshall: Marshall[AuthCmd, TaskId, HttpRequest] =
        jsonMarshall[ExecutionOp](HttpMethod.Get, cmd => s"$TaskExecutionsURI/${cmd.payload}", writeBody = false)

      override val unmarshall: Unmarshall[HttpResponse, Option[TaskExecution]] = Unmarshall { res =>
        if (res.statusCode == 404) LawfulTry.success(none[TaskExecution])
        else super.unmarshall(res).asInstanceOf[LawfulTry[Option[TaskExecution]]]
      }
    }

    override implicit val scheduleOp: ScheduleOp = new JsonUnmarshall[ScheduleOp] with ScheduleOp {
      override val marshall: Marshall[AuthCmd, ScheduleJob, HttpRequest] =
        jsonMarshall[ScheduleOp](HttpMethod.Put, _ => ExecutionPlansURI)
    }

    override implicit val cancelPlanOp: CancelPlanOp = new JsonUnmarshall[CancelPlanOp] with CancelPlanOp {
      override val marshall: Marshall[AuthCmd, PlanId, HttpRequest] =
        jsonMarshall[CancelPlanOp](HttpMethod.Delete, cmd => s"$ExecutionPlansURI/${cmd.payload}", writeBody = false)
    }
  }

  val ops = new HttpClusterOps with HttpRegistryOps with HttpSchedulerOps with HttpSecurityOps {}
}

object HttpProtocol extends HttpProtocol
