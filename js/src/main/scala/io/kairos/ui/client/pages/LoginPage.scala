package io.kairos.ui.client.pages

import io.kairos.ui.client.SiteMap.ConsolePage
import io.kairos.ui.client.security.LoginForm
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 13/10/2015.
 */
object LoginPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(
      width(300 px),
      height(300 px),
      position.absolute,
      left(50 %%),
      top(50 %%),
      marginLeft(-150 px),
      marginTop(-150 px)
    )
  }

  private[this] val component = ReactComponentB[RouterCtl[ConsolePage]]("LoginPage").
    stateless.
    noBackend.
    render((p, _, _) => {
      <.div(Style.content,
        <.div(^.`class` := "panel panel-default",
          <.div(^.`class` := "panel-heading", "Sign in into Kairos Console"),
          <.div(^.`class` := "panel-body", LoginForm(p))
        )
      )
    }
  ).build

  def apply(router: RouterCtl[ConsolePage]) = component(router)

}
