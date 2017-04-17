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
import java.time.{LocalDate, LocalDateTime, LocalTime, ZonedDateTime}

import cron4s.expr.CronExpr

import io.quckoo._

import japgolly.scalajs.react.ReactNode
import japgolly.scalajs.react.extra.Reusability

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
  implicit lazy val symbolReuse             = Reusability.by[Symbol, String](_.name)
  implicit lazy val timeUnitReuse           = Reusability.byRef[TimeUnit]
  implicit lazy val finiteDurationReuse     = Reusability.byRef[FiniteDuration]
  implicit lazy val localDateReuse          = Reusability.byRef[LocalDate]
  implicit lazy val localTimeReuse          = Reusability.byRef[LocalTime]
  implicit lazy val localDateTimeReuse      = Reusability.byRef[LocalDateTime]
  implicit lazy val zonedDateTimeReuse      = Reusability.byRef[ZonedDateTime]
  implicit lazy val artifactIdReuse         = Reusability.caseClass[ArtifactId]
  implicit lazy val cronExprReuse           = Reusability.by[CronExpr, String](_.toString)

  implicit lazy val jarJobPackageReuse: Reusability[JarJobPackage] =
    Reusability.caseClass[JarJobPackage]
  implicit lazy val shellScriptPackageReuse: Reusability[ShellScriptPackage] =
    Reusability.caseClass[ShellScriptPackage]
  implicit lazy val jobPackageReuse: Reusability[JobPackage] =
    Reusability.either[JarJobPackage, ShellScriptPackage].contramap {
      case jar: JarJobPackage        => Left(jar)
      case shell: ShellScriptPackage => Right(shell)
    }

  implicit lazy val immediateTriggerReuse = Reusability.byRef[Trigger.Immediate.type]
  implicit lazy val afterTriggerReuse     = Reusability.caseClass[Trigger.After]
  implicit lazy val everyTriggerReuse     = Reusability.caseClass[Trigger.Every]
  implicit lazy val atTriggerReuse        = Reusability.caseClass[Trigger.At]
  implicit lazy val cronTriggerReuse      = Reusability.caseClass[Trigger.Cron]

  implicit lazy val triggerReuse: Reusability[Trigger] =
    Reusability.either[Trigger.Immediate.type, Either[Trigger.After, Either[Trigger.Every, Either[Trigger.At, Trigger.Cron]]]]
      .contramap {
        case Trigger.Immediate    => Left(Trigger.Immediate)
        case after: Trigger.After => Right(Left(after))
        case every: Trigger.Every => Right(Right(Left(every)))
        case at: Trigger.At       => Right(Right(Right(Left(at))))
        case cron: Trigger.Cron   => Right(Right(Right(Right(cron))))
      }

  implicit def toReactNode(notification: Notification): ReactNode =
    notification.inline

}
