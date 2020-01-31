/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console.core

import java.time.Clock

import cats.implicits._

import diode._
import diode.react.ReactConnector

import io.quckoo.auth.InvalidCredentials
import io.quckoo.client.ClientIO
import io.quckoo.client.http.{HttpQuckooClient, JSQuckooClient}
import io.quckoo.console.ConsoleRoute
import io.quckoo.console.dashboard.DashboardHandler
import io.quckoo.console.registry.RegistryHandler
import io.quckoo.console.scheduler.{ExecutionPlansHandler, SchedulerHandler, TasksHandler}
import io.quckoo.protocol.Event

import slogging.LazyLogging

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 20/02/2016.
  */
object ConsoleCircuit
    extends Circuit[ConsoleScope] with ReactConnector[ConsoleScope] with ConsoleOps
    with ConsoleSubscriptions with LazyLogging {

  object Implicits {
    implicit val consoleClock: Clock = Clock.systemDefaultZone
  }

  override protected val client: HttpQuckooClient = JSQuckooClient()

  override protected def initialModel: ConsoleScope = ConsoleScope.initial

  override protected def actionHandler = composeHandlers(
    loginHandler,
    new DashboardHandler(zoomTo(_.clusterState), this),
    new RegistryHandler(zoomTo(_.userScope.jobSpecs), this),
    new SchedulerHandler(zoomTo(_.userScope), this),
    new ExecutionPlansHandler(zoomTo(_.userScope.executionPlans), this),
    new TasksHandler(zoomTo(_.userScope.executions), this),
    globalHandler
  )

  val globalHandler: HandlerFunction = (model, action) =>
    action match {
      case Growl(notification) =>
        notification.growl()
        None

      case StartClusterSubscription =>
        if (!model.subscribed) {
          model.clientState.passport.map { implicit passport =>
            logger.debug("Opening console subscription channels...")
            openSubscriptionChannels(client)
            ActionResult.ModelUpdate(model.copy(subscribed = true))
          }
        } else None
    }

  val loginHandler = new ConsoleHandler(zoomTo(_.clientState)) {

    def performLogin(
        username: String,
        password: String,
        referral: Option[ConsoleRoute]
    ): ClientIO[Event] =
      client
        .signIn(username, password)
        .map(_ => LoggedIn(referral).asInstanceOf[Event])
        .onError {
          case ex => logger.error("Unexpected error when performing login.", ex).pure[ClientIO]
        }
        .recover { case InvalidCredentials => LoginFailed }

    override def handle = {
      case Login(username, password, referral) =>
        runClientIO(performLogin(username, password, referral))

      case Logout =>
        runClientIO(client.signOut.map(_ => LoggedOut))
    }
  }

}
