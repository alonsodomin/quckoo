package io.kairos.console.client.core

import diode.{Circuit, Dispatcher}
import diode.data.AsyncAction.PendingException
import diode.data._
import io.kairos.JobSpec
import io.kairos.console.auth.User
import io.kairos.console.client.security.ClientAuth
import io.kairos.id.JobId

import scala.util.{Failure, Try}

/**
  * Created by alonsodomin on 20/02/2016.
  */
case class KairosModel private (currentUser: Option[User], jobSpecs: PotMap[JobId, JobSpec])

case class LoggedIn(username: String)
case object LoggedOut

object KairosModel extends ClientAuth {

  def initial(circuit: Circuit[KairosModel]) =
    KairosModel(
      currentUser = authInfo.map(auth => User(auth.userId)),
      jobSpecs = PotMap(new JobSpecFetch(circuit))
    )

}
