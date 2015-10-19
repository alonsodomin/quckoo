package io.kairos.console.client

import org.scalajs.dom.raw.HTMLStyleElement

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
package object layout {

  object GlobalStyle extends StyleSheet.Standalone {
    import dsl._

    "html" - (
      position.relative,
      minHeight(100 %%)
    )

    "body" - marginBottom(60 px)

    ".footer" - (
      position.absolute,
      bottom(0 px),
      left(0 px),
      width(100 %%),
      height(60 px),
      backgroundColor(Color("#f8f8f8"))
    )

    val contents: HTMLStyleElement = render(cssStyleElementRenderer, env)

  }

}
