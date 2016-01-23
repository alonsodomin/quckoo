package io.kairos.console.client.core

import io.kairos.console.client.security.ClientAuth
import io.kairos.console.info.ClusterInfo
import io.kairos.console.protocol.LoginRequest
import io.kairos.console.{Api, RegistryApi}
import io.kairos.id.JobId
import io.kairos.serialization._
import io.kairos.{JobSpec, Validated}
import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
object ClientApi extends Api with RegistryApi with ClientAuth {
  import dom.console
  import dom.ext.Ajax

  private[this] val BaseURI = "/api"
  private[this] val LoginURI = BaseURI + "/login"
  private[this] val LogoutURI = BaseURI + "/logout"

  private[this] val ClusterDetailsURI = BaseURI + "/cluster/info"

  private[this] val RegistryBaseURI = BaseURI + "/registry"
  private[this] val JobsURI = RegistryBaseURI + "/jobs"

  private[this] val JsonRequestHeaders = Map("Content-Type" -> "application/json")

  override def login(username: String, password: String)(implicit ec: ExecutionContext): Future[Unit] = {
    import upickle.default._

    Ajax.post(LoginURI, write(LoginRequest(username, password)), headers = JsonRequestHeaders).
      map { xhr => () }
  }

  override def logout()(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.post(LogoutURI, headers = authHeaders) map { xhr => () }
  }

  override def clusterDetails(implicit ex: ExecutionContext): Future[ClusterInfo] = {
    import upickle.default._

    Ajax.get(ClusterDetailsURI, headers = authHeaders) map { xhr =>
      read[ClusterInfo](xhr.responseText)
    }
  }

  override def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[Validated[JobId]] = {
    import upickle.default._

    Ajax.post(JobsURI, write(jobSpec), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      console.log(xhr.responseText)
      read[Validated[JobId]](xhr.responseText)
    }
  }

  override def enabledJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    import upickle.default._

    println("Fetching registered jobs from the backend...")

    Ajax.get(JobsURI, headers = authHeaders).map { xhr =>
      read[Map[JobId, JobSpec]](xhr.responseText)
    }
  }

}
