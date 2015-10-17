package io.kairos.ui.client

import japgolly.scalajs.react.React
import org.scalajs.dom.raw.{HTMLStyleElement, HTMLElement}

import scalacss.Defaults._
import scalacss.Env
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
package object layout {

  object GlobalStyle extends StyleSheet.Standalone {
    import dsl._

    private[this] val footerHeight = height(100 px)

    ".view-port" - (
      minHeight(100 %%),
      marginBottom(100 px),

      &.after - (
        content := "",
        display.block,
        footerHeight
      )
    )

    ".footer" - (
      backgroundColor.white,
      footerHeight
    )

    val contents: HTMLStyleElement = render(cssStyleElementRenderer, env)

  }

}
