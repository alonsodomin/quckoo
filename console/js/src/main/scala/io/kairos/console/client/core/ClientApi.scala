package io.kairos.console.client.core

import io.kairos.JobSpec
import io.kairos.console.client.security.ClientAuth
import io.kairos.console.info.ClusterInfo
import io.kairos.console.protocol.LoginRequest
import io.kairos.console.{Api, RegistryApi}
import io.kairos.id.JobId
import org.scalajs.dom.ext.Ajax

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
object ClientApi extends Api with RegistryApi with ClientAuth {

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

  override def getJobs()(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    import upickle.default._

    Ajax.get(JobsURI, headers = authHeaders).map { xhr =>
      read[Map[JobId, JobSpec]](xhr.responseText)
    }
  }

}
