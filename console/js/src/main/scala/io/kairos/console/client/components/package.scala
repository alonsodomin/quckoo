package io.kairos.console.client

import japgolly.scalajs.react.ReactNode
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 20/02/2016.
  */
package object components {
  import scala.language.implicitConversions

  implicit def icon2VDom(icon: Icon): ReactNode = {
    <.span(^.classSet1M("fa", icon.classSet), ^.paddingRight := 5.px)
  }

}
