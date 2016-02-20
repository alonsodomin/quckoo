package io.kairos.console.client.core

import diode.data._
import io.kairos.JobSpec
import io.kairos.console.auth.User
import io.kairos.id.JobId

/**
  * Created by alonsodomin on 20/02/2016.
  */
case class KairosModel private (currentUser: Option[User])

case class LoggedIn(username: String)
case object LoggedOut

object KairosModel {
  def initial = KairosModel(Some(User("stupid")))
}
