/*
 * Copyright 2016 Antonio Alonso Dominguez
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

import diode._

import io.quckoo.console.ConsoleRoute
import io.quckoo.console.components.Notification

import japgolly.scalajs.react.extra.router.RouterCtl

import org.threeten.bp.{Clock, LocalDateTime}

import slogging.LazyLogging

import scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 26/03/2016.
  */
class LoginProcessor(routerCtl: RouterCtl[ConsoleRoute])
    extends ActionProcessor[ConsoleScope] with LazyLogging {

  import ConsoleRoute._
  import ActionResult._

  val clock = Clock.systemUTC

  val authFailedNotification = Notification.danger("Username or password incorrect")

  override def process(dispatch: Dispatcher, action: Any,
                       next: Any => ActionResult[ConsoleScope],
                       currentModel: ConsoleScope): ActionResult[ConsoleScope] = {
    action match {
      case LoginFailed =>
        logger.warn("Login failed!")
        EffectOnly(Growl(authFailedNotification))

      case LoggedIn(passport, referral) =>
        val destination = referral.getOrElse(DashboardRoute)
        val newModel = currentModel.copy(
          passport = Some(passport),
          lastLogin = Some(LocalDateTime.now(clock))
        )
        logger.info("Successfully logged in! Redirecting to {}", destination)
        ModelUpdateEffect(newModel, NavigateTo(destination))

      case LoggedOut =>
        logger.info("Successfully logged out.")
        val action = Effect.action(NavigateTo(DashboardRoute))
        ModelUpdateEffect(currentModel.copy(passport = None), action)

      case NavigateTo(route) =>
        routerCtl.set(route).runNow()
        NoChange

      case _ => next(action)
    }
  }

}
