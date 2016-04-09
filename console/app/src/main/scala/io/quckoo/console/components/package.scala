package io.quckoo.console

import java.util.concurrent.TimeUnit

import io.quckoo.Trigger
import io.quckoo.time.{DateTime, MomentJSDate, MomentJSTime}
import japgolly.scalajs.react.ReactNode
import japgolly.scalajs.react.extra.Reusability
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.jquery.JQuery

import scala.concurrent.duration.FiniteDuration
import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
package object components {
  import scala.language.implicitConversions

  val lookAndFeel = new LookAndFeel

  // React's reusability instances for common types
  implicit val timeUnitReuse = Reusability.byRef[TimeUnit]
  implicit val finiteDurationReuse = Reusability.byRef[FiniteDuration]
  implicit val dateReuse = Reusability.byRef[MomentJSDate]
  implicit val timeReuse = Reusability.byRef[MomentJSTime]
  implicit val dateTimeReuse = Reusability.byRef[DateTime]

  implicit val immediateTriggerReuse = Reusability.byRef[Trigger.Immediate.type]
  implicit val afterTriggerReuse = Reusability.caseClass[Trigger.After]
  implicit val everyTriggerReuse = Reusability.caseClass[Trigger.Every]
  implicit val atTriggerReuse = Reusability.caseClass[Trigger.At]

  implicit def icon2VDom(icon: Icon): ReactNode = {
    <.span(^.classSet1M("fa", icon.classSet), icon.state.padding ?= (^.paddingRight := 5.px))
  }

  implicit def jq2bootstrap(jq: JQuery): BootstrapJQuery = jq.asInstanceOf[BootstrapJQuery]

  implicit def jq2Notify(jq: JQuery): BootstrapNotify = jq.asInstanceOf[BootstrapNotify]

}
