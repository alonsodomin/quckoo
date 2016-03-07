package io.kairos.console.client.core

import diode.data._
import io.kairos.{Faults, JobSpec}
import io.kairos.console.auth.User
import io.kairos.console.client.security.ClientAuth
import io.kairos.id.JobId

/**
  * Created by alonsodomin on 20/02/2016.
  */

object JobSpecFetch extends Fetch[JobId] {

  override def fetch(key: JobId): Unit =
    KairosCircuit.dispatch(UpdateJobSpecs(keys = Set(key)))

  override def fetch(keys: Traversable[JobId]): Unit =
    KairosCircuit.dispatch(UpdateJobSpecs(keys.toSet))

}

case class RegistryModel private (
    jobSpecs: PotMap[JobId, JobSpec],
    lastErrors: Option[Faults]
)
object RegistryModel {

  def initial: RegistryModel = RegistryModel(
    jobSpecs = PotMap(JobSpecFetch),
    lastErrors = None
  )

}

case class KairosModel private (currentUser: Option[User], registry: RegistryModel)

case class LoggedIn(username: String)
case object LoggedOut

object KairosModel extends ClientAuth {

  def initial =
    KairosModel(
      currentUser = authInfo.map(auth => User(auth.userId)),
      registry = RegistryModel.initial
    )

}
