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

import diode._
import diode.react.ReactConnector

import io.quckoo.auth.Session
import io.quckoo.client.http.HttpQuckooClient
import io.quckoo.client.http.dom._
import io.quckoo.console.dashboard.DashboardHandler
import io.quckoo.console.registry.RegistryHandler
import io.quckoo.console.scheduler.{ExecutionPlansHandler, SchedulerHandler, TasksHandler}

import slogging.LazyLogging

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

  protected val client: HttpQuckooClient = HttpDOMQuckooClient

  override protected def initialModel: ConsoleScope = ConsoleScope.initial

  override protected def actionHandler = composeHandlers(
    new SecurityHandler(zoomTo(_.session), this),
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
          model.session match {
            case Session.Authenticated(passport) =>
              logger.debug("Opening console subscription channels...")
              openSubscriptionChannels(client)
              Some(ActionResult.ModelUpdate(model.copy(subscribed = true)))

            case _ => None
          }
        } else None

      case ClientResult(newSession, result) =>
        Some(
          ActionResult
            .ModelUpdateSilentEffect(model.copy(session = newSession), Effect.action(result))
        )
  }

}
