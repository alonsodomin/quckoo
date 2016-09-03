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

package io.quckoo.console

import java.util.concurrent.TimeUnit

import io.quckoo.Trigger
import io.quckoo.id.ArtifactId

import japgolly.scalajs.react.ReactNode
import japgolly.scalajs.react.extra.Reusability

import org.scalajs.jquery.{JQuery, JQueryStatic}

import org.threeten.bp.{LocalDate, LocalDateTime, LocalTime, ZonedDateTime}

import scalacss.Defaults._
import scala.concurrent.duration.FiniteDuration
import scala.language.implicitConversions

/**
  * Created by alonsodomin on 20/02/2016.
  */
package object components {
  val lookAndFeel = new LookAndFeel

  // Unicode character for non-breaking-space in HTML
  final val NBSP = "\u00a0"

  // React's reusability instances for common types
  implicit val timeUnitReuse = Reusability.byRef[TimeUnit]
  implicit val finiteDurationReuse = Reusability.byRef[FiniteDuration]
  implicit val localDateReuse = Reusability.byRef[LocalDate]
  implicit val localTimeReuse = Reusability.byRef[LocalTime]
  implicit val localDateTimeReuse = Reusability.byRef[LocalDateTime]
  implicit val zonedDateTimeReuse = Reusability.byRef[ZonedDateTime]
  implicit val artifactIdReuse = Reusability.byRef[ArtifactId]

  implicit val immediateTriggerReuse = Reusability.byRef[Trigger.Immediate.type]
  implicit val afterTriggerReuse = Reusability.caseClass[Trigger.After]
  implicit val everyTriggerReuse = Reusability.caseClass[Trigger.Every]
  implicit val atTriggerReuse = Reusability.caseClass[Trigger.At]

  implicit def toReactNode(notification: Notification): ReactNode =
    notification.inline

  // JQuery plugins

  implicit def toBootstrapJQuery(jq: JQuery): BootstrapJQuery =
    jq.asInstanceOf[BootstrapJQuery]

  implicit def jq2Notify(jq: JQueryStatic): BootstrapNotify =
    jq.asInstanceOf[BootstrapNotify]

}
