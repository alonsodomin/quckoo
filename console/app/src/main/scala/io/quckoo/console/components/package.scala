package io.quckoo.console

import japgolly.scalajs.react.ReactNode
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.jquery.JQuery
import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
package object components {
  import scala.language.implicitConversions

  val lookAndFeel = new LookAndFeel

  implicit def icon2VDom(icon: Icon): ReactNode = {
    <.span(^.classSet1M("fa", icon.classSet), icon.state.padding ?= (^.paddingRight := 5.px))
  }

  implicit def jq2bootstrap(jq: JQuery): BootstrapJQuery = jq.asInstanceOf[BootstrapJQuery]

}
